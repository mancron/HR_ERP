package com.hrms.org.dto;

public class DeptDTO {

	private int dept_id;
	private String dept_name;
	private int parent_dept_id;
	private int manager_id;
	private int dept_level;
	private int sort_order;
	private int is_active;
	private String closed_at;
	private String created_at;
	
	public DeptDTO() {}
	
	public int getDept_id() {
		return dept_id;
	}
	public void setDept_id(int dept_id) {
		this.dept_id = dept_id;
	}
	public String getDept_name() {
		return dept_name;
	}
	public void setDept_name(String dept_name) {
		this.dept_name = dept_name;
	}
	public int getParent_dept_id() {
		return parent_dept_id;
	}
	public void setParent_dept_id(int parent_dept_id) {
		this.parent_dept_id = parent_dept_id;
	}
	public int getManager_id() {
		return manager_id;
	}
	public void setManager_id(int manager_id) {
		this.manager_id = manager_id;
	}
	public int getDept_level() {
		return dept_level;
	}
	public void setDept_level(int dept_level) {
		this.dept_level = dept_level;
	}
	public int getSort_order() {
		return sort_order;
	}
	public void setSort_order(int sort_order) {
		this.sort_order = sort_order;
	}
	public int getIs_active() {
		return is_active;
	}
	public void setIs_active(int is_active) {
		this.is_active = is_active;
	}
	public String getClosed_at() {
		return closed_at;
	}
	public void setClosed_at(String closed_at) {
		this.closed_at = closed_at;
	}
	public String getCreated_at() {
		return created_at;
	}
	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}
}
