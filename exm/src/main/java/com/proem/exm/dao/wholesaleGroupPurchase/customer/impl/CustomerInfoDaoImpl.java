package com.proem.exm.dao.wholesaleGroupPurchase.customer.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.cisdi.ctp.datamng.impl.BaseDataMngImpl;
import com.proem.exm.dao.wholesaleGroupPurchase.customer.CustomerInfoDao;
import com.proem.exm.utils.Page;

/**
 * 客户档案
 * @author ZuoYM 
 * @com proem
 */
@Repository("customerInfoDao")
public class CustomerInfoDaoImpl extends BaseDataMngImpl implements CustomerInfoDao {
	@Override
	public List<Map<String, Object>> getObjPagedList(Page page)
			throws Exception {
		// TODO Auto-generated method stub
		List<Map<String, Object>> list = this.querySql(Page.getPagedSQL(page.getSql(), page.getPage(), page.getRows()), null);
		return list;
	}

	@Override
	public Long getObjListCount(Page page) throws Exception {
		// TODO Auto-generated method stub
		Long count = 0L;
		List<Map<String, Object>> list = null;
		list = this.querySql(Page.getListCountSQL(page.getSql()), null);
		Map<String, Object> map = (Map<String, Object>) list.get(0);
		BigDecimal bd = (BigDecimal) map.get("COUNT");
		count = bd.longValue();
		return count;
	}
}
