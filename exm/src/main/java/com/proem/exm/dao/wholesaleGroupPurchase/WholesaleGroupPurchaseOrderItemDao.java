package com.proem.exm.dao.wholesaleGroupPurchase;

import java.util.List;
import java.util.Map;

import com.proem.exm.utils.Page;

public interface WholesaleGroupPurchaseOrderItemDao {
	public List<Map<String, Object>> getObjPagedList(Page page)
			throws Exception;

	public Long getObjListCount(Page page) throws Exception;
}
