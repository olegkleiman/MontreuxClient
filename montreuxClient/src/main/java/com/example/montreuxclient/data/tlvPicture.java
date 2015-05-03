package com.example.montreuxclient.data;

public class tlvPicture {
	@com.google.gson.annotations.SerializedName("id")
	public String Id;
	
	@com.google.gson.annotations.SerializedName("link")
    private String Link;
	public String getLink() {
		return Link;
	}
	public void setLink(String userName){
		Link = userName;
	}
	
	@com.google.gson.annotations.SerializedName("boothid")
    private int BoothID;
	public int getBoothID(){
		return BoothID;
	}
	public void setBoothID(int value){
		BoothID = value;
	}
}
