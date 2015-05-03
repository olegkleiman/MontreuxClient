package com.example.montreuxclient;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.UUID;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.widget.Toast;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.Ignore;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableResult;
import com.microsoft.azure.storage.table.TableServiceEntity;

public class AzureStore {

	public static class UploadParams {
		
		public UploadParams() {
			this(null, "");
		}
		
		public UploadParams(File file, String containerName) {
			this.file = file;
			this.containerName = containerName;
		}
		
		File file;
		public File getFile() {
			return file;
		}
		public void setFile(File file){
			this.file = file;
		}
		
		String containerName;
		public String getContainerName() {
			return containerName;
		}
		public void setContainerName(String name){
			containerName = name;
		}
	}
	
	public static class UploadBlobTask extends AsyncTask<UploadParams, String, Boolean> {
		
		Context context;
		Exception error;
		URI publishedUri;
		
		UploadTableTask continuationTask;
		ReportEntity report;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=telaviv;" + 
			    "AccountKey=oJXjRr6HRBf2k0oX3/IlE+qZkfI1r5p5pvm8vsa3WjpbyZ36GIBVjKL746yjYrQaxSq/kd1K0QHLUEiFZgBUGw==";

		public UploadBlobTask(Context ctx, 
							UploadTableTask continuationTask,
							ReportEntity report){
			context = ctx;
			this.continuationTask = continuationTask;
			this.report = report;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
			if( !result  && error != null ) {
				String strMessage = error.getMessage();
				Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show(); 
			}	
			else
			{
				if( continuationTask != null ){
					
					report.setBlobURL(publishedUri.toString());
					continuationTask.execute(report);
				}
		    	
			}
		}
		
		@Override
		protected Boolean doInBackground(UploadParams... params) {
			
			try {
				
				String containerName = params[0].getContainerName();
				File photoFile = params[0].getFile();
				
				if( photoFile != null ) {
					CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
					CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
					CloudBlobContainer container = blobClient.getContainerReference(containerName);

					String fileName = photoFile.getName();
				    CloudBlockBlob blob = container.getBlockBlobReference(fileName);

				    blob.upload(new FileInputStream(photoFile), photoFile.length());
				    
				    publishedUri = blob.getQualifiedUri();
				}
				 
			} catch(Exception e) {
				error = e;
				return false;
			}
			
			return true;
		}
	}
	
    // POJO
    public static class ReportEntity extends TableServiceEntity {
    	
    	public static final int REPORT_TYPE_UNKNOWN = 0;
    	public static final int REPORT_TYPE_TABLE_ENTITY = 1;
    	public static final int REPORT_TYPE_BLOG = 2;
    	
    	public ReportEntity(String topic, String userName, String message, 
    						Location l, int reportType, String blobURL) {

    		this.userName = userName;
    		this.message = message;
    		this.reportType = reportType;
    		this.blobURL = blobURL;
 
    		this.setLongitude( l.getLongitude());
    		this.setLatitude( l.getLatitude() );
 		
    		this.tableName = "montreux";
    		this.seen = false;
    		
    		this.partitionKey = topic;
    		UUID uuid = UUID.randomUUID();
    	    this.rowKey = uuid.toString();
    	}
    	
    	public ReportEntity() 
    	{
    		this("", "", "", null, ReportEntity.REPORT_TYPE_UNKNOWN, "");
    	}
    	
    	public String userName;
    	public String getUserName() {
    		return userName;
    	}
    	public void setUserName(String userName){
    		this.userName = userName;
    	}
  
    	public String message;
    	public String getMessage() {
    		return message;
    	}
    	public void setMessage(String message) {
    		this.message = message;
    	}
    	
    	private double latitude;
    	public void setLatitude(double l) {
    		latitude = l;
    	}
    	public double getLatitude() {
    		return latitude;
    	}
    	
    	private double longitude;
    	public void setLongitude(double l) {
    		longitude = l;
    	}
    	public double getLongitude() {
    		return longitude;
    	}
    	
    	private int reportType;
    	public int getReportType() {
    		return reportType;
    	}
    	public void setReportType(int type){
    		reportType = type;
    	}
    	
    	private String blobURL;
    	public String getBlobURL() {
    		return blobURL;
    	}
    	public void setBlobURL(String blob) {
    		blobURL = blob;
    	}
    	
    	private Boolean seen;
    	public Boolean getSeen() {
    		return seen;
    	}
    	public void setSeen(Boolean seen) {
    		this.seen = seen;
    	}
    	
    	private String tableName;
    	@Ignore // prevents from serialization
    	public void setTableName(String tableName) {
    		this.tableName = tableName;
    	}
    	@Ignore // prevents from serialization
    	public String getTableName() {
    		return tableName;
    	}
    }
	
	public static class UploadTableTask extends AsyncTask<ReportEntity, String, Boolean> {
		
		Context context;
		Exception error;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=telaviv;" + 
			    "AccountKey=oJXjRr6HRBf2k0oX3/IlE+qZkfI1r5p5pvm8vsa3WjpbyZ36GIBVjKL746yjYrQaxSq/kd1K0QHLUEiFZgBUGw==";

		public UploadTableTask(Context ctx){
			context = ctx;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
			if( !result  && error != null ) {
				String strMessage = error.getMessage();
				Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show(); 
			}	
			else
		    	Toast.makeText(context, "uploaded", Toast.LENGTH_LONG).show();
		}
		
		@Override
		protected Boolean doInBackground(ReportEntity... params) {
			
			try {

				ReportEntity report = (ReportEntity)params[0];
	
				CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				CloudTableClient tableClient = storageAccount.createCloudTableClient();
				CloudTable table = tableClient.getTableReference(report.getTableName());
				
				table.createIfNotExists();

				TableOperation insertOperation = TableOperation.insert(report);
				TableResult result = table.execute(insertOperation);
				
				@SuppressWarnings("unused")
				int httpStatus = result.getHttpStatusCode();
				
				// Test!!!
				// The TableQuery is strong typed and must be instantiated 
				// with a class type that is accessible and contains a nullary constructor
//				TableOperation retrieveOp = TableOperation.retrieve(report.getUserName(), "", ReportEntity.class);
//				
//				// Submit the operation to the table service and get the specific entity.
//				@SuppressWarnings("unused")
//				ReportEntity savedReport = table.execute(retrieveOp).getResultAsType(); 
//				
//		        // Retrieve all entities in a partition.
//		        // Create a filter condition t match the partition key
//		        String partitionFilter = TableQuery.generateFilterCondition(//TableConstants.PARTITION_KEY, 
//		        							"PartitionKey",
//		        							QueryComparisons.EQUAL, 
//		        							report.getPartitionKey());
//
//		        // Specify a partition query
//		        TableQuery<ReportEntity> partitionQuery = TableQuery.from(ReportEntity.class).where(partitionFilter);
//		        
//		        // Loop through the results, displaying information about the entity.
//		        for (ReportEntity entity : table.execute(partitionQuery)) {
//		        	Log.i(LOG_TAG, entity.getUserName() + " " + entity.getMessage());
//		        }
				
			} catch(StorageException e) {
				error = e;
				return false;
			} catch(URISyntaxException e) {
				error = e;
				return false;
			} catch (InvalidKeyException e) {
				error = e;
				return false;
			} catch(Exception e) {
				error = e;
				return false;				
			}
			
			return true;
		}
	}
}
