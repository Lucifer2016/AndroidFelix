/**
 * 
 * @author bxs3514
 *
 * This is a android felix launcher.
 *
 * @lastEdit 11/23/2014
 * 
 */

package afelix.service.controler.felixcontrol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.felix.framework.Felix;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import android.content.Context;
import android.util.Log;

public class FelixControler implements BundleControler{
	final private static String TAG = "FelixControler";
	
	private Felix felixFramework;
	private Bundle resBundle;
	private ConsoleInterpreter mInterpreter;
	
	private String res = new String();
	private boolean su = false;
	
	public FelixControler(Felix felixFramework){
		this.felixFramework = felixFramework;
		this.mInterpreter = new ConsoleInterpreter(this);
	}
	
	@Override
	public String install(Context context, String bundle, int command){
		Log.d(TAG, "About to install bundle: " + bundle);
		String res = this.MainControler(context, bundle, command);
		return res;
	}
	
	@Override
	public String install(String bundle, String location){
		Log.d(TAG, "About to install bundle: " + bundle);
		res = this.MainControler(bundle, location, 2);
		return res;
	}
	
	@Override
	public String uninstall(String bundle){
		Log.d(TAG, "About to uninstall bundle: " + bundle);
		res = this.MainControler(bundle, "", 1);
		return res;
	}
	
	@Override
	public String start(String bundle){
		Log.d(TAG, "About to start bundle: " + bundle);
		res = this.MainControler(bundle, "", 32);
		return res;
	}
 
	@Override
	public String stop(String bundle){
		Log.d(TAG, "About to start bundle: " + bundle);
		res = this.MainControler(bundle, "", 16);
		return res;
	}
	

	@Override
	public String find(String bundle) {
		res = this.MainControler(bundle, "", 0);
		return res;
	}
	
	
	public String interpret(String command){
		return mInterpreter.interpret(command);
	}

	public void setSu(boolean su) {
		this.su = su;
	}
	
	public Bundle getResBundle(String bundle){
		find(bundle);
		return resBundle;
	}
	
	private String MainControler(String bundle, String location, int command){
		
		//Main command execution block
		switch(command){
		case 2://install bundle
			FileInputStream bs = null;
			
			try{
				Log.d(TAG, "Get bundle at: " + location);
				File f = null;
					
				//Escaping when users forget to input '/'
				if(location.charAt(location.length()-1) == '/')
					f = new File(location + bundle);
				else f = new File(location + "/" + bundle);
				
				bs = new FileInputStream(f);
			}catch(IOException ie){
				Log.e(TAG, "File open fail for " + ie.toString(), ie);
				return "File open failed.";
			}
				
			try{
				this.felixFramework.getBundleContext().installBundle(bundle, bs);
			}catch(BundleException be){
				Log.e(TAG, "Bundle "+ bundle + "installed fail for " + be.toString(), be);
				return "Bundle "+ bundle + "installed fail for " + be.toString();
			}
				
			try{
				bs.close();
			}catch(IOException ie){
				Log.e(TAG, "Close the inputstream fail for " + ie.toString(), ie);
				return "Fail to close the file.";
			}
				
			return "Bundle:" + bundle + " has installed successfully";
		case 0://find bundle
		case 1://uninstall a bundle
		case 16://stop a bundle
		case 32://start a bundle
        	//Log.e(TAG, "!!!!!!222222");
			long bid = -1;
			boolean isLong = false;
			
			for(int i = 0; i < bundle.length(); i++){
				Log.d(TAG, String.valueOf(bundle.charAt(i)));
				if(bundle.charAt(i) < '0' || bundle.charAt(i) > '9') break;
				if(i == bundle.length() - 1){
					bid = Long.parseLong(bundle);
					if(bid == 0 && !su) {
						resBundle = felixFramework.getBundleContext().getBundle(0);
						return "Permission denied.";
					}
					isLong = true;
				}
			}
			
			if(!isLong){
				org.osgi.framework.Bundle[] bl = felixFramework.getBundleContext().getBundles();
		        for (int i = 0; bl != null && i < bl.length; i++) {
		        	org.osgi.framework.Bundle tempBundle = bl[i];
		        	Log.d(TAG, bundle+"?"+tempBundle.getBundleId());
		            if (bundle.equals(tempBundle.getBundleId()) || bundle.equals(tempBundle.getLocation())  
		            		|| bundle.equals(tempBundle.getSymbolicName())) {
		                bid = tempBundle.getBundleId();
		            }
		        }
			}

	        org.osgi.framework.Bundle b = felixFramework.getBundleContext().getBundle(bid);
	        if (b == null) {
	        	Log.e(TAG, "The bundle " + bundle + " doesn't exist.");
	            return "The bundle " + bundle + " doesn't exist.";
	        }
	        
	        //ServiceTracker st = null;
	        switch(command){
	        case 0:
	        	resBundle = b;
	        	return "Bundle: " + b.getSymbolicName() + " is found."; 
	        case 1:
	        	try {
		            b.uninstall();
		            Log.d(TAG, "bundle: " + b.getSymbolicName() + "/" + b.getBundleId() + "/"
		                    + b + " has uninstalled from felix.");
		            return "Bundle: " + bundle + " has installed successfully";
		        } catch (BundleException be) {
		        	Log.e(TAG, be.toString(), be);
		        	return "Unable to uninstall Bundle: " + bundle + " for\n" + be.toString();
		        }
	        case 16:
	        	try {
		            b.stop(org.osgi.framework.Bundle.RESOLVED);
		            
		            Log.d(TAG, "bundle: " + b.getSymbolicName() + "/" + b.getBundleId() + "/"
		                    + b + " stopped");
		        	return "Bundle: " + bundle + " has stoped successfully";
		        } catch (BundleException be) {
		        	Log.e(TAG, be.toString(), be);
		        	return "Unable to stop Bundle: " + bundle + " for\n" + be.toString();
		        }
	        case 32:
	        	try {
	        		
		            b.start(org.osgi.framework.Bundle.START_ACTIVATION_POLICY);
	        		//b.getRegisteredServices();
		            
		            //Log.e(TAG, "!!!!!!" + b.getServicesInUse().toString());
		            Log.d(TAG, "bundle: " + b.getSymbolicName() + "/" + b.getBundleId() + "/"
		                    + b + " started");
		            return "Bundle: " + bundle + " has started successfully";
		        } catch (BundleException be) {
		        	System.out.println(be.toString());
		        	return "Unable to start Bundle: " + bundle + " for\n" + be.toString();
		        }
				
	        }
		default:
			return "Invalid command.";   
		}
	}
	
	private String MainControler(Context context, String bundle, int command){
		switch(command){
			case 2:
				InputStream bs = null;
				
				try{
					bs =  context.getAssets().open("bundle/" + bundle);
				}catch(IOException ie){
					Log.e(TAG, "Get assests inputstream fail for " + ie.toString(), ie);
					return "Fail";
				}
				
				try{
					this.felixFramework.getBundleContext().installBundle(bundle, bs);
				}catch(BundleException be){
					Log.e(TAG, "Bundle "+ bundle + "installed fail for " + be.toString(), be);
					return "Fail";
				}
				
				try{
					bs.close();
				}catch(IOException ie){
					Log.e(TAG, "Close the inputstream fail for " + ie.toString(), ie);
					return "Fail";
				}
				break;
			case 32:
				break;
			default:
				return "Invalid command.";
		}
		return "Success";
	}
	
	@Override
	public ArrayList<String> BundleInfo(int command){
		ArrayList<String> as = new ArrayList<String>();
		
		for(org.osgi.framework.Bundle b : felixFramework.getBundleContext().getBundles()) {
			switch(command){
			case 1:
				as.add(Long.toString(b.getBundleId()));
				break;
			case 2:
				as.add(b.getSymbolicName());
				break;
			case 4:
				switch(b.getState()){
				case 2:
					as.add(b.getBundleId()+" \t\t" + b.getSymbolicName() + "\t\t INSTALLED");
					break;
				case 4:
					as.add(b.getBundleId()+" \t\t" + b.getSymbolicName() + "\t\t RESOLVED");
					break;
				case 8:
					as.add(b.getBundleId()+" \t\t" + b.getSymbolicName() + "\t\t STARTING");
					break;
				case 16:
					as.add(b.getBundleId()+" \t\t" + b.getSymbolicName() + "\t\t STOPPING");
					break;
				case 32:
					as.add(b.getBundleId()+" \t\t" + b.getSymbolicName() + "\t\t ACTIVE");
				}
				break;
			default:
				Log.e(TAG, "Invalid command.");
			}
		}
		return as;
	}

	
}
