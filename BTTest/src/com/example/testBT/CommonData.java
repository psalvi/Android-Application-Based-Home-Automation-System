package com.example.testBT;





import LibPack.ValuesDB;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class CommonData {

	public static boolean cancel = false, running = false,
			sendToServer = false;
	public static ValuesDB vdb;
	public static boolean valueRequested = false;
	public static String error = null, urlstr = "", ip;
	public static BluetoothClientService mChatService = null;
	public static Context context = null;

	public static class LongOperation extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			while (!cancel) {
			
				while (running) {
				
					byte[] b = new byte[] { (byte) vdb.opcode,
							(byte) vdb.operand };
					sendMessage(b);
					if (vdb.opcode == 34) {
					
					}
					
					valueRequested = true;
					if (vdb.opcode != 73) {
						vdb.opcode = 73;
						vdb.operand = vdb.currentChannel;
					}
					try {
						Thread.sleep(500);
					} catch (Exception e) {
						System.out.println("Error in long Operation : " + e);
					}

				}

			}
			return null;
		}
	}

	public static void sendMessage(byte[] send) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothClientService.STATE_CONNECTED) {
			Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (send.length > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			mChatService.write(send);
		}
	}

}
