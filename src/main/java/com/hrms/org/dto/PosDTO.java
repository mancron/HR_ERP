package com.hrms.org.dto;

import java.math.BigDecimal;

public class PosDTO {

	private int position_id;
	private String position_name;
	private int	position_level;
	private BigDecimal base_salary;
	private int meal_allowance;
	private int transport_allowance;
	private int position_allowance;
	private int is_active;
	private String created_at;
	private int emp_count;
	
	public PosDTO() {}
	
	public int getPosition_id() {
		return position_id;
	}
	public void setPosition_id(int position_id) {
		this.position_id = position_id;
	}
	public String getPosition_name() {
		return position_name;
	}
	public void setPosition_name(String position_name) {
		this.position_name = position_name;
	}
	public int getPosition_level() {
		return position_level;
	}
	public void setPosition_level(int position_level) {
		this.position_level = position_level;
	}
	public BigDecimal getBase_salary() {
		return base_salary;
	}
	public void setBase_salary(BigDecimal base_salary) {
		this.base_salary = base_salary;
	}
	public int getMeal_allowance() {
		return meal_allowance;
	}
	public void setMeal_allowance(int meal_allowance) {
		this.meal_allowance = meal_allowance;
	}
	public int getTransport_allowance() {
		return transport_allowance;
	}
	public void setTransport_allowance(int transport_allowance) {
		this.transport_allowance = transport_allowance;
	}
	public int getPosition_allowance() {
		return position_allowance;
	}
	public void setPosition_allowance(int position_allowance) {
		this.position_allowance = position_allowance;
	}
	public int getIs_active() {
		return is_active;
	}
	public void setIs_active(int is_active) {
		this.is_active = is_active;
	}
	public String getCreated_at() {
		return created_at;
	}
	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}
	public int getEmp_count() {
        return emp_count;
    }
    public void setEmp_count(int emp_count) {
        this.emp_count = emp_count;
    }
}
