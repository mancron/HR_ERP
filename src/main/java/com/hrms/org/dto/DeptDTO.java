package com.hrms.org.dto;

public class DeptDTO {

    private int    dept_id;
    private String dept_name;
    private int    parent_dept_id;
    private int    manager_id;
    private String manager_name;   // [추가] JOIN으로 채워지는 부서장 이름 (DB 컬럼 아님)
    private int    dept_level;
    private int    sort_order;
    private int    is_active;
    private String closed_at;
    private String created_at;

    public int    getDept_id()        { return dept_id; }
    public void   setDept_id(int v)   { this.dept_id = v; }

    public String getDept_name()          { return dept_name; }
    public void   setDept_name(String v)  { this.dept_name = v; }

    public int    getParent_dept_id()       { return parent_dept_id; }
    public void   setParent_dept_id(int v)  { this.parent_dept_id = v; }

    public int    getManager_id()       { return manager_id; }
    public void   setManager_id(int v)  { this.manager_id = v; }

    public String getManager_name()          { return manager_name; }
    public void   setManager_name(String v)  { this.manager_name = v; }

    public int    getDept_level()       { return dept_level; }
    public void   setDept_level(int v)  { this.dept_level = v; }

    public int    getSort_order()       { return sort_order; }
    public void   setSort_order(int v)  { this.sort_order = v; }

    public int    getIs_active()       { return is_active; }
    public void   setIs_active(int v)  { this.is_active = v; }

    public String getClosed_at()          { return closed_at; }
    public void   setClosed_at(String v)  { this.closed_at = v; }

    public String getCreated_at()          { return created_at; }
    public void   setCreated_at(String v)  { this.created_at = v; }
}