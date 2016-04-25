package com.proem.exm.controller.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.cisdi.ctp.utils.common.UuidUtils;
import com.proem.exm.entity.basic.associator.Associator;
import com.proem.exm.entity.basic.goodsFile.GoodsFile;
import com.proem.exm.entity.order.ZcOrder;
import com.proem.exm.entity.order.ZcOrderItem;
import com.proem.exm.entity.system.ZcZoning;
import com.proem.exm.service.basic.associator.AssociatorService;
import com.proem.exm.service.basic.goodsFileService.GoodsFileService;
import com.proem.exm.service.order.OrdersItemService;
import com.proem.exm.service.order.OrdersService;
import com.proem.exm.service.system.LogManageService;
import com.proem.exm.service.system.ZcZoningService;
import com.proem.exm.utils.Constant;
import com.proem.exm.utils.JdbcUtil;
import com.proem.exm.utils.StringUtil;


public class QuartzController  {
	    SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		@Autowired
		OrdersService ordersService;
		@Autowired
		ZcZoningService zcZoningService;
		@Autowired
		OrdersItemService ordersItemService;
		@Autowired
		GoodsFileService goodsFileService;
		@Autowired
		AssociatorService associatorService;
	    protected void execute() throws SQLException  {  
	    	Connection conn=JdbcUtil.getConnection();
	    	try {
	    		ZcOrder zcOrder=Constant.ZC_ORDER;
	    		if(zcOrder.getOrderDate()==null){
	    			  Date startDate=new Date();
	    			  startDate.setHours(8);
		    		  startDate.setDate(new Date().getDay()-1);
		    		  zcOrder.setOrderDate(startDate);  
	    		}else{
	    			Calendar calendar = Calendar.getInstance(); //得到日历
	    			calendar.setTime(new Date());
	    			calendar.add(Calendar.DAY_OF_MONTH, -1);
	    			zcOrder.getOrderDate().setYear(new Date().getYear());
	    			zcOrder.getOrderDate().setMonth(calendar.get(Calendar.MONTH));
		    		zcOrder.getOrderDate().setDate(calendar.get(Calendar.DAY_OF_MONTH)); 
	    		}
	    		if(zcOrder.getUpdateTime()==null){
	    			Date endDate=new Date();
	    			endDate.setHours(8);
	    			zcOrder.setUpdateTime(endDate);
	    		}else{
	    			Calendar calendar = Calendar.getInstance(); //得到日历
	    			calendar.setTime(new Date());
	    			zcOrder.getUpdateTime().setYear(new Date().getYear());
	    			zcOrder.getUpdateTime().setMonth(calendar.get(Calendar.MONTH));
		    		  zcOrder.getUpdateTime().setDate(calendar.get(Calendar.DAY_OF_MONTH));
	    		}
	    		String conditionStr=joinCondition(zcOrder);
				selectFromJDBC(conn,conditionStr);
			} catch (Exception e) {
				e.printStackTrace();
				conn.close();
			}finally{
				conn.close();
			}
	    }
	    private String joinCondition(Object obj){
			ZcOrder orders = (ZcOrder) obj;
			String conditions = "and a.status !='dead' ";
//			if(StringUtil.validate(orders.getOrderCome())){
//				conditions += " and a.source='" + orders.getOrderCome() + "'";
//			}
			if(StringUtil.validate(orders.getOrderDate())){
				String startDate=sdf.format(orders.getOrderDate());
				conditions += " and a.createtime >= UNIX_TIMESTAMP('" + startDate + "')";
			}
			if(StringUtil.validate(orders.getUpdateTime())){
				String endDate=sdf.format(orders.getUpdateTime());
				conditions += " and a.createtime <= UNIX_TIMESTAMP('" + endDate + "')";
			}
			return conditions;
		}
	    /**
		 * 送数据库中读取订单数据进行拉取
		 * @param con
		 * @param conditionStr
		 */
		public void selectFromJDBC(Connection con, String conditionStr) {
			PreparedStatement ps = null;
			ResultSet rs = null;
			try

			{

				String sql = "SELECT a.order_id,a.final_amount,a.status,FROM_UNIXTIME(a.createtime,'%Y-%m-%d %H:%i:%s'), a.source,a.ship_name,a.ship_mobile,a.ship_addr, a.total_amount,a.discount, a.pmt_order ,a.member_id,a.itemnum ,a.ship_area FROM sdb_b2c_orders  a where 1=1 ";
				sql += conditionStr;
				ps = con.prepareStatement(sql);
				rs = ps.executeQuery(sql);

				int i = 1;
				while (rs.next()) {
					String orderNum = rs.getString(1);
					String condition = "orderNum='" + orderNum + "'";
					Long count = ordersService.getCountByObj(ZcOrder.class,
							condition);
					if (count == 0) {
						InsertItem(1,con, orderNum);
						ZcOrder zcOrder = new ZcOrder();
						ZcZoning zcZoning = new ZcZoning();
						String zoningId = UuidUtils.getUUID();
						zcZoning.setId(zoningId);
						zcZoning.setStreet(rs.getString(8));
						zcZoningService.saveObj(zcZoning);
						String id = UuidUtils.getUUID();
						zcOrder.setId(rs.getString(1));
						zcOrder.setOrderNum(rs.getString(1));
						zcOrder.setOrderTotalValue(rs.getFloat(2));
						zcOrder.setOrderStatus("1");
						Date orderDate = sdf.parse(rs.getString(4));
						zcOrder.setOrderDate(orderDate);
						zcOrder.setOrderCome("1");
						zcOrder.setConsignee(rs.getString(6));
						zcOrder.setCansignPhone(rs.getString(7));
						zcOrder.setZcZoning(zcZoning);
						zcOrder.setOrderAmount(rs.getFloat(9));
						zcOrder.setOrderReduceAmount(rs.getFloat(10));
						zcOrder.setIsGift(rs.getString(11));
						String memberId=rs.getString(12);
						Long countMember=associatorService.getCountByObj(Associator.class, "id='"+memberId+"'");
						if(countMember==0){
							InsertItem(2,con, rs.getString(12));
							Associator associator=(Associator) associatorService.getObjById(memberId, "Associator");
							zcOrder.setAssociator(associator);
						}else{
							Associator associator=(Associator) associatorService.getObjById(memberId, "Associator");
							zcOrder.setAssociator(associator);
						}
						zcOrder.setMemberCardNumber(rs.getString(12));
						zcOrder.setGoodsNum(rs.getInt(13));
						String area=rs.getString(14);
						String[] areaStr=area.split(":");
						String areaId ="";
						if(areaStr!=null && areaStr.length==3){
							areaId= areaStr[2];
						}
						zcOrder.setBranchId(areaId);
						ordersService.saveObj(zcOrder);
						System.out.println("------------" + i);
					} else {
						// TODO 如果重复的是保存还是更新
					}
					i++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (null != rs) {
					try {
						rs.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if (null != ps) {
					try {
						ps.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}

		public void InsertItem(int type,Connection con, String orderId) {
			PreparedStatement ps = null;
			ResultSet rs = null;
			String sql="";
			try {
				if(type==1){
					sql = " SELECT item_id,obj_id,product_id,goods_id,type_id,bn,name,cost,price,g_price,amount,score,weight,nums,sendnum,addon,item_type  FROM sdb_b2c_order_items  where order_id='"
						+ orderId + "' ";
				}
				if(type==2){
					sql="SELECT b.value,a.member_id,a.name,a.member_lv_id,a.point_history,a.point_freeze,a.point,a.advance,a.advance_freeze , "+
							"FROM_UNIXTIME(a.regtime,'%Y-%m-%d %H:%i:%s'),a.sex,a.wedlock,(a.`b_year`+a.`b_month`+a.`b_day`) AS birthday,a.tel,a.mobile,a.email,c.addr,a.zip,a.remark FROM sdb_b2c_members a "+
							"LEFT JOIN sdb_dbeav_meta_value_varchar b ON b.pk=a.member_id  "+
							"LEFT JOIN sdb_b2c_member_addrs c ON c.member_id=a.member_id where a.member_id='"+orderId+"'";
				}
				ps = con.prepareStatement(sql);
				rs = ps.executeQuery(sql);
				while (rs.next()) {
					if(type==1){
					ZcOrderItem zcOrderItem = new ZcOrderItem();
					zcOrderItem.setId(rs.getString(1));
					zcOrderItem.setOrder_id(orderId);
					zcOrderItem.setObj_id(rs.getString(1));
					zcOrderItem.setProduct_id(rs.getString(3));
					String goodId = rs.getString(6);
					GoodsFile goodsFile = (GoodsFile) goodsFileService.getObjById(
							goodId, "GoodsFile");
					zcOrderItem.setGoodsFile(goodsFile);
					zcOrderItem.setType_id(rs.getString(5));
					zcOrderItem.setBn(rs.getString(6));
					zcOrderItem.setName(rs.getString(7));
					zcOrderItem.setCost(rs.getString(8));
					zcOrderItem.setPrice(rs.getString(9));
					zcOrderItem.setG_price(rs.getString(10));
					zcOrderItem.setAmount(rs.getString(11));
					zcOrderItem.setScore(rs.getString(12));
					zcOrderItem.setWeight(rs.getString(13));
					zcOrderItem.setNums(rs.getString(14));
					zcOrderItem.setGoodsState("1");
					zcOrderItem.setSendNum(rs.getString(15));
					// zcOrderItem.setAddon(rs.getFloat(16));
					zcOrderItem.setItem_type(rs.getString(17));
					ordersItemService.saveObj(zcOrderItem);
					}
					if(type==2){
						Associator associator=new Associator();
						associator.setAssociator_CardNumber(rs.getString(1));
						associator.setId(rs.getString(2));
						associator.setAssociator_Name(rs.getString(3));
						associator.setAssociator_Category(rs.getString(4));
						associator.setAssociator_AccumulatedCredit(rs.getInt(5));
						associator.setAssociator_UsedCredit(rs.getInt(6));
						associator.setAssociator_Credit(rs.getInt(7));
						associator.setAssociator_Amount(rs.getDouble(8));
						associator.setAssociator_ConsumeAmount(rs.getDouble(9));
						Date regDate = sdf.parse(rs.getString(10));
						associator.setAssociator_AdmissionDate(regDate);
						associator.setAssociator_Sex(rs.getString(11));
						associator.setAssociator_MaritalStatus(rs.getString(12));
						//设置生日
//						associator.setAssociator_Birthday(rs.getDate(13));
						associator.setAssociator_Telephone(rs.getString(14));
						associator.setAssociator_Mobilephone(rs.getString(15));
						associator.setAssociator_Email(rs.getString(16));
						associator.setAssociator_Address(rs.getString(17));
						associator.setAssociator_Zipcode(rs.getString(18));
						associator.setAssociator_Note(rs.getString(19));
						associatorService.saveObj(associator);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
