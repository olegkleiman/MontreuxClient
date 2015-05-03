package com.example.montreuxclient.data;

import java.util.Date;

public class tlvVisit {

	@com.google.gson.annotations.SerializedName("id")
	public String Id;
	
	@com.google.gson.annotations.SerializedName("username")
    private String UserName;
	public String getUserName() {
		return UserName;
	}
	public void setUserName(String userName){
		UserName = userName;
	}
	
	@com.google.gson.annotations.SerializedName("boothid")
    private int BoothID;
	public int getBoothID(){
		return BoothID;
	}
	public void setBoothID(int value){
		BoothID = value;
	}
	
	@com.google.gson.annotations.SerializedName("whenvisited")
	private Date WhenVisited;
	public Date getWhenVisited(){
		return WhenVisited;
	}
	public void setWhenVisited(Date value){
		WhenVisited = value;
	}
}
