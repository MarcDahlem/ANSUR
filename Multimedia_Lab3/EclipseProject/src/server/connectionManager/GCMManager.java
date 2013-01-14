package server.connectionManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import server.app.Server;

import com.google.android.gcm.server.*;
import com.google.android.gcm.server.Message.Builder;

public class GCMManager {
	
	public static void sendToDevices(List<String> receiverRegIDs, Map<String, String> payload) throws IOException{
		
		if (receiverRegIDs == null || receiverRegIDs.isEmpty()) {
			//no one to inform...
			return;
		}
		Sender sender = new Sender(Server.GCM_SERVER_API_KEY);
		Builder builder = new Message.Builder();
		
		for (Entry<String,String> entry:payload.entrySet()) {
			builder = builder.addData(entry.getKey(), entry.getValue());	
		}
		
		Message message = builder.build();
		MulticastResult multiresult = sender.send(message, receiverRegIDs, 5);
		
		
		//TODO check the results from the server
		for (Result result:multiresult.getResults()){
			if (result.getMessageId() != null) {
				 String canonicalRegId = result.getCanonicalRegistrationId();
				 if (canonicalRegId != null) {
				   // same device has more than on registration ID: update database
				 }
				} else {
				 String error = result.getErrorCodeName();
				 if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				   // application has been removed from device - unregister database
				 }
				}
		}
	}
}
