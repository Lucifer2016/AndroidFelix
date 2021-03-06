
/**
 * 
 * @author bxs3514
 *
 * This is a monitor to show the bundle states to users.
 *
 * @lastEdit 07/16/2015
 * 
 */

package afelix.monitor.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import afelix.afelixservice.androidfelix.R;
import afelix.service.controler.database.BundleDataCenter;
import afelix.service.controler.database.DatabaseControler;
import afelix.service.interfaces.IAFelixService;
import afelix.service.net.SocketTransfer;
import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AFelixActivity extends ActionBarActivity implements OnClickListener{

	final private static String TAG = "AFelixActivity";
	
	private ServiceConnection mConnection;
	private IAFelixService mAFelixService;
	
	private DatabaseControler afDbCtrl;
	
	private ListView BundleList;
	private TextView speedText;
	private EditText Command;
	private Button ConfirmBtn;
	private Button ResetBtn;
	private Button RefreshBtn;
	private Button ShowAllBundleBtn;
	private Intent bindServiceIntent;
	private Intent showBundleIntent;
	private Bundle getInstallBundle;

	private ArrayList<?> installList;
	private ArrayList<String> bundleInstallList;
	private ArrayList<String> as;
	private ArrayAdapter<String> mArrayAdapter;
	private HashMap<Integer, String> installBundleMap;
	private HashMap<String, Integer> bundleIdMap;
	private String[] bundles;
	private String[] operations;
	private String commandStore;
	
	private Thread refreshThread;
	private RefreshList refresh;
	private TimerTask refreshNetworkSpeed;
	private Timer timer;
	private float[] uploadSpeed;
	private float[] downloadSpeed;
	private float avgUploadSpeed;
	private float avgDownloadSpeed;
    private int counter = 0;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_afelix);
		//dbCenter = new BundleDataCenter();
		
		initViews();
		buildServiceConnection();

		bindServiceIntent = new Intent(IAFelixService.class.getName());
		
		bindServiceIntent = this.createExplicitFromImplicitIntent(
				getApplicationContext(), bindServiceIntent);

		bindService(bindServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	
	@Override
	protected void onStart(){
		super.onStart();
	}
	
	
	@Override
	protected void onResume(){
		super.onResume();
	}
	
	
	@Override
	protected void onRestart(){
		super.onRestart();
		Command.setText(commandStore);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		commandStore = Command.getText().toString();
	}
	
	@Override
	protected void onStop(){
		super.onStop();
	}
	
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(mConnection != null)
			unbindService(mConnection);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		switch (resultCode){
		case RESULT_OK:
			if(data.getExtras() != null){
				//Bundle b = data.getExtras();
				
				getInstallBundle = data.getExtras();
				installList = getInstallBundle.getParcelableArrayList("installBundle");
				installBundleMap = (HashMap<Integer, String>) installList.get(0);
				for(Iterator<?> i = installBundleMap.entrySet().iterator(); i.hasNext();){
					Map.Entry temp = (Map.Entry)i.next();
					bundleInstallList.add((String)temp.getValue());
					try {
						if((String)temp.getValue() != null && !((String)temp.getValue()).equals("")){
							String bundleName = ((String)temp.getValue()).substring(
									((String)temp.getValue()).length() 
									- ((String)temp.getValue()).split("\\s+")[1].length());
							mAFelixService.installBundleByLocation(bundleName, null);
							bundleIdMap.put(bundleName, Integer.valueOf(((String)temp.getValue()).split("\\s+")[0]));
						}
							//mAFelixService.interpret("install " + ((String)temp.getValue()).split("\\s+")[1]);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				refresh.run();
			}
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.confirm:
			try {
				if(mAFelixService.interpret(Command.getText().toString())){
					if(refreshThread.getState() == Thread.State.NEW)
						refreshThread.start();
					Command.setText("");
				}
			} catch (RemoteException e) {
				
				e.printStackTrace();
			}
			break;
		case R.id.refresh:
			if(refreshThread.getState() == Thread.State.NEW)
				refreshThread.start();
			break;
		case R.id.reset:
			Command.setText("");
			break;
		case R.id.showAllBundleList:
			showBundleIntent = new Intent(AFelixActivity.this, BundleDataCenter.class);
			this.startActivityForResult(showBundleIntent, 1);
			break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.afelix, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/** Handle action bar item clicks here. The action bar will
		automatically handle clicks on the Home/Up button, so long
		as you specify a parent activity in AndroidManifest.xml.*/
		int id = item.getItemId();
		if (id == R.id.Socket) {
			LayoutInflater layoutInflater = LayoutInflater.from(this);
			View socketView = layoutInflater.inflate(R.layout.socket_setting, null);
			final EditText[] ipEdit = new EditText[]{(EditText)socketView.findViewById(R.id.ipEdit1),
					(EditText)socketView.findViewById(R.id.ipEdit2),
					(EditText)socketView.findViewById(R.id.ipEdit3),
					(EditText)socketView.findViewById(R.id.ipEdit4)};
			final EditText portEdit = (EditText)socketView.findViewById(R.id.portEdit);
			
			new AlertDialog.Builder(this).setTitle("Socket Setting").setView(socketView).
			setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						mAFelixService.setSocket(ipEdit[0].getText().toString() + "." + 
								ipEdit[1].getText().toString() + "." + 
								ipEdit[2].getText().toString() + "." + 
								ipEdit[3].getText().toString(),
								Integer.parseInt(portEdit.getText().toString()));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					
				}
			}).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
				}
			}).show();
			
			return true;
		}
		else if(id == R.id.NetSpeedOpinion){
			measureUploadSpeed();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void buildServiceConnection(){
		
		mConnection = new ServiceConnection(){

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				
				try{
					if(service.getInterfaceDescriptor().equals(IAFelixService.class.getName())){
						mAFelixService = IAFelixService.Stub.asInterface(service);
						refreshThread.start();
					}
				}catch (RemoteException re){
					re.printStackTrace();
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				
				Toast.makeText(AFelixActivity.this, "Android Felix Service has disconnected", 
						Toast.LENGTH_LONG).show();
				mAFelixService = null;
			}
			
		};
	}
	
	
	private Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
		//Change a implicit intent to a explicit one
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
 
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
 
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
 
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
 
        // Set the component to be explicit
        explicitIntent.setComponent(component);
 
        return explicitIntent;
    }
	
	private void initViews(){
		//Init views
		bundleInstallList = new ArrayList<String>();
		bundleIdMap = new HashMap<String, Integer>();
		afDbCtrl = new DatabaseControler(getApplicationContext());
		ArrayList<String> as = new ArrayList<String>();
		as.add(getApplicationContext().getPackageManager()
				.getApplicationLabel(getApplicationContext().getApplicationInfo()).toString());
		afDbCtrl.Insert("App", as);
		as.clear();
		
		//
		speedText = (TextView)findViewById(R.id.networkSpeed);
		uploadSpeed = new float[10];
		downloadSpeed = new float[10];
		timer = new Timer();

		//
		operations = new String[]{"Start", "Stop", "Update", "Restart", "Uninstall", "Send", "Dependency"};
		BundleList = (ListView)findViewById(R.id.bundleList);
		BundleList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent,
					View view, int position, long id) { 
				
				final String bundle = (String)parent.getItemAtPosition(position);
				final String bundleId = (bundle.split("\\s+"))[0];
				
				new AlertDialog.Builder(AFelixActivity.this)
				.setTitle("Choose your operation to the bundle")
				.setItems(operations, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						switch(which){
						case 0:
							try {
								mAFelixService.interpret("start " + bundleId);
								refresh.run();
							} catch (RemoteException e) {
								
								e.printStackTrace();
							}
							break;
						case 1:
							try {
								mAFelixService.interpret("stop " + bundleId);
								refresh.run();
							} catch (RemoteException e) {
								
								e.printStackTrace();
							}
							break;
						
						case 2:
							try {
								mAFelixService.interpret("update " + bundleId);
								refresh.run();
							} catch (RemoteException e) {
								
								e.printStackTrace();
							}
							break;
						case 3:
							try {
								mAFelixService.interpret("restart " + bundleId);
								refresh.run();
							} catch (RemoteException e) {
								
								e.printStackTrace();
							}
							break;
						case 4:
							try {
								mAFelixService.interpret("uninstall " + bundleId);
								refresh.run();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
							break;
						case 5:
							try {
								mAFelixService.sendBundle(afDbCtrl.Query(new String[]{"FileName"}, 
										"File", "Bundle=\"" + (bundle.split("\\s+"))[1] + "\"").get(0).entrySet().iterator().next().getValue());
								afDbCtrl.clearQuery();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
							break;
						case 6:
							try {
								mAFelixService.dependency((bundle.split("\\s+"))[0]);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
							break;
						}
					}
				}).show();
				
			}
		});
		
		refresh = new RefreshList();
		refreshThread = new Thread(refresh);
		
		Command = (EditText)findViewById(R.id.command);
		Command.setOnEditorActionListener(new TextView.OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
						|| (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction())){
					try {
						if(mAFelixService.interpret(Command.getText().toString())){
							//Refresh();
							refreshThread.run();
							Command.setText("");
						}
						//else 
							//Toast.makeText(AFelixActivity.this, "Your command is wrong!", Toast.LENGTH_SHORT).show();
					} catch (RemoteException e) {
						
						e.printStackTrace();
					}
				}
				
				return true;
			}
			
		});
		
		ConfirmBtn = (Button)findViewById(R.id.confirm);
		ConfirmBtn.setOnClickListener(this);
		
		ResetBtn = (Button)findViewById(R.id.reset);
		ResetBtn.setOnClickListener(this);
		
		RefreshBtn = (Button)findViewById(R.id.refresh);
		RefreshBtn.setOnClickListener(this);
		
		ShowAllBundleBtn = (Button)findViewById(R.id.showAllBundleList);
		ShowAllBundleBtn.setOnClickListener(this);
	}
	
	private void measureUploadSpeed(){
		counter = 0;
		avgUploadSpeed = avgDownloadSpeed = 0;
		
		//if(refreshNetworkSpeed != null){
			//refreshNetworkSpeed.cancel();
			//refreshNetworkSpeed = new mNetworkSpeed();
		//}
		refreshNetworkSpeed = new mNetworkSpeed();
		timer.schedule(refreshNetworkSpeed, 0, 1000);
	}
	
	private class mNetworkSpeed extends TimerTask{

		@Override
		public void run(){
			try{
				synchronized(this){
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							try {
								speedText.setText(mAFelixService.networkSpeed());
								uploadSpeed[counter] = mAFelixService.networkUploadSpeed();
								downloadSpeed[counter] = mAFelixService.networkDownloadSpeed();
								avgUploadSpeed += uploadSpeed[counter];
								avgDownloadSpeed += downloadSpeed[counter];
								counter++;
								System.out.println(counter);
								if(counter == 10) {
									avgUploadSpeed /= 10;
									avgDownloadSpeed /= 10;
									
									speedText.setText("Upload Speed: " + avgUploadSpeed + "KB/s\n" 
									+ "Download Speed: " + avgDownloadSpeed + "KB/s");
									refreshNetworkSpeed.cancel();
									//timer.cancel();
								}
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
						
					});
				}
			}catch(IllegalThreadStateException te){
				Log.e(TAG, te.toString());
			}
		}
		
		
	}
	
	
	private class RefreshList implements Runnable{
		
		private void Refresh(){
			try {
				as = (ArrayList<String>)mAFelixService.getAll();
				bundles = as.toArray(new String[as.size()]);
				mArrayAdapter = new ArrayAdapter<String>(AFelixActivity.this, 
						android.R.layout.simple_list_item_1, bundles);
				BundleList.setAdapter(mArrayAdapter);
				
			} catch (RemoteException e) {
				
				Log.e(TAG, "Service has unexpected disconnected.", e);
				e.printStackTrace();
			}
		}
		

		@Override
		public void run() {
			try{
				synchronized(this){
					//this.wait();
					
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Refresh();
						}
						
					});
				}
			}catch(IllegalThreadStateException te){
				Log.e(TAG, te.toString());
			}
		}
	}
}
