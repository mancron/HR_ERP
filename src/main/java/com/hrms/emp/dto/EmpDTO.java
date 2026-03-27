package com.hrms.emp.dto;

public class EmpDTO {
	
	private int emp_id; 			//직원고유 ID
	private String emp_name; //직원 이름
	private String emp_no;		 //사번
	private int dept_id;			 //소속 부서
	private int position_id; 	//직급
	private String hire_date; 	//입사일
	private String resign_date; //퇴사일
	private String emp_type; //정규직,계약직,파트타임
	private String status; 		//재식,휴직,퇴직
	private int base_salary;	//개인 기본급
	private String birth_date; //생년월일
	private String gender;		//성별
	private String address;		//주소
	private String emergency_contact; //긴급연락처
	private String bank_account;	//급여 이체 계좌번호
	private String email;		//회사 이메일
	private String phone;		//연락처
	private String created_at;//등록일시
	
	public EmpDTO() {}
	
	public int getEmp_id() {
		return emp_id;
	}
	public void setEmp_id(int emp_id) {
		this.emp_id = emp_id;
	}
	public String getEmp_name() {
		return emp_name;
	}
	public void setEmp_name(String emp_name) {
		this.emp_name = emp_name;
	}
	public String getEmp_no() {
		return emp_no;
	}
	public void setEmp_no(String emp_no) {
		this.emp_no = emp_no;
	}
	public int getDept_id() {
		return dept_id;
	}
	public void setDept_id(int dept_id) {
		this.dept_id = dept_id;
	}
	public int getPosition_id() {
		return position_id;
	}
	public void setPosition_id(int position_id) {
		this.position_id = position_id;
	}
	public String getHire_date() {
		return hire_date;
	}
	public void setHire_date(String hire_date) {
		this.hire_date = hire_date;
	}
	public String getResign_date() {
		return resign_date;
	}
	public void setResign_date(String resign_date) {
		this.resign_date = resign_date;
	}
	public String getEmp_type() {
		return emp_type;
	}
	public void setEmp_type(String emp_type) {
		this.emp_type = emp_type;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getBase_salary() {
		return base_salary;
	}
	public void setBase_salary(int base_salary) {
		this.base_salary = base_salary;
	}
	public String getBirth_date() {
		return birth_date;
	}
	public void setBirth_date(String birth_date) {
		this.birth_date = birth_date;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getEmergency_contact() {
		return emergency_contact;
	}
	public void setEmergency_contact(String emergency_contact) {
		this.emergency_contact = emergency_contact;
	}
	public String getBank_account() {
		return bank_account;
	}
	public void setBank_account(String bank_account) {
		this.bank_account = bank_account;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getCreated_at() {
		return created_at;
	}
	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}
}
