package com.andrideng.plugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;
import android.app.PendingIntent;

import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import HPRTAndroidSDK.HPRTPrinterHelper;
import HPRTAndroidSDK.IPort;
import print.Print;
import print.PublicFunction;
// import rx.functions.Action1;

public class HprtPrinter extends CordovaPlugin implements SensorEventListener {
  // - Hprt Printer
  private static final String ACTION_USB_PERMISSION = "android.com.andrideng.plugin";
  private Context thisCon;

  private UsbManager mUsbManager = null;
  private UsbDevice device = null;
  private PendingIntent mPermissionIntent;

  private ExecutorService executorService;
  private PublicAction PAct = null;
  private PublicFunction PFun = null;
  private static IPort Printer = null;

  private BluetoothAdapter mBluetoothAdapter;

  // - Temperature
  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;

  public long TIMEOUT = 30000;

  int status;
  long timeStamp;
  long lastAccessTime;

  SensorManager sensorManager;
  Sensor sensor;

  Float temperature;
  private CallbackContext callbackContext;
  
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    this.thisCon = cordova.getActivity().getApplicationContext();
    this.mPermissionIntent = PendingIntent.getBroadcast(this.thisCon, 0, new Intent(ACTION_USB_PERMISSION), 0);
    this.executorService = Executors.newSingleThreadExecutor();
    this.PAct = new PublicAction(this.thisCon);
    this.PFun = new PublicFunction(this.thisCon);

    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    this.thisCon.registerReceiver(this.mUsbReceiver, filter);

    this.InitSetting();
    // this.EnableBluetooth();
  }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      this.callbackContext = callbackContext;

      if (action.equals("checkTemperature")) {
         this.start();
         return true;
      }
  
      if (action.equals("isDeviceCompatible")) {
         this.isDeviceCompatible();
         return true;
      }

      if ("connectUsb".equals(action)) {
        this.connect(callbackContext);
        return true;
      }

      if ("printSample".equals(action)) {
        this.printSample();
        return true;
      }

      if ("cutPaper".equals(action)) {
        this.cutPaper();
        return true;
      }

      if ("customPrint".equals(action)) {
        String message = args.getString(0);
        this.customPrint(message, callbackContext);
        return true;
      }

      return false;
    }

  private void InitSetting() {
		String SettingValue="";
		SettingValue=PFun.ReadSharedPreferencesData("Codepage");
		if(SettingValue.equals(""))		
			PFun.WriteSharedPreferencesData("Codepage", "0,PC437(USA:Standard Europe)");			
		
		SettingValue=PFun.ReadSharedPreferencesData("Cut");
		if(SettingValue.equals(""))		
			PFun.WriteSharedPreferencesData("Cut", "0");	//
			
		SettingValue=PFun.ReadSharedPreferencesData("Cashdrawer");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Cashdrawer", "0");
					
		SettingValue=PFun.ReadSharedPreferencesData("Buzzer");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Buzzer", "0");
					
		SettingValue=PFun.ReadSharedPreferencesData("Feeds");
		if(SettingValue.equals(""))			
			PFun.WriteSharedPreferencesData("Feeds", "0");				
	}

  private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
	{
	    public void onReceive(Context context, Intent intent) 
	    {
	    	try {
		        String action = intent.getAction();	       
		        if (ACTION_USB_PERMISSION.equals(action)){
			        synchronized (this) 
			        {		        	
			            device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                  if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                  {			 
                    if(Print.PortOpen(thisCon, device) != 0)
                    {					
                        // txtTips.setText(thisCon.getString(R.string.activity_main_connecterr));
                        Log.e("DEBUG", "MAIN ACTIVITY ERROR CONNECT BOSS!");
                        return;
                    }
                    else {
                      // txtTips.setText(thisCon.getString(R.string.activity_main_connected));
                      Log.e("DEBUG", "MAIN ACTIVITY CONNECTED");
                    }
                  }		
                  else
                  {			   
                    Log.e("DEBUG", "ELSE USB PERMISSION!");     	
                    return;
                  }
			        }
			      }
          
          if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
          {
            device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null)
            {
              int count = device.getInterfaceCount();
              for (int i = 0; i < count; i++)
              {
                UsbInterface intf = device.getInterface(i);
                //Class ID 7代表打印机
                if (intf.getInterfaceClass() == 7)
                {
                  Print.PortClose();
                  // txtTips.setText(R.string.activity_main_tips);
                  Log.e("DEBUG", "Port close call!");
                }
              }
            }
				  }
			}catch (Exception e){
	    		Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> mUsbReceiver ")).append(e.getMessage()).toString());
	    	}
		}
	};

  //EnableBluetooth
	private boolean EnableBluetooth(){
        boolean bRet = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter != null) {
            if(mBluetoothAdapter.isEnabled())
                return true;
            mBluetoothAdapter.enable();
            try 
            {
              Thread.sleep(500);
            } 
            catch (InterruptedException e) 
            {			
              e.printStackTrace();
            }
            if(!mBluetoothAdapter.isEnabled())
            {
                bRet = true;
                Log.d("PRTLIB", "BTO_EnableBluetooth --> Open OK");
            }
        } 
        else
        {
        	Log.d("HPRTSDKSample", (new StringBuilder("Activity_Main --> EnableBluetooth ").append("Bluetooth Adapter is null.")).toString());
        }
        return bRet;
    }

  public void connect(CallbackContext callbackContext) {
    try {
      // Toast.makeText(cordova.getActivity(), "We are in the connect function", Toast.LENGTH_LONG).show();
      mUsbManager = (UsbManager) thisCon.getSystemService(Context.USB_SERVICE);
      HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
      Log.d("test", deviceList.toString());
      Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

      boolean HavePrinter = false;
      while(deviceIterator.hasNext())
      {
        device = deviceIterator.next();
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++)
        {
          UsbInterface intf = device.getInterface(i);
          if (intf.getInterfaceClass() == 7)
          {
            Log.d("PRINT_TAG", "vendorID--"
                + device.getVendorId() + "ProductId--"
                + device.getProductId() + "ProductName: "
                + device.getProductName() + "DeviceName: "
                + device.getDeviceName() + device.getManufacturerName()
            );
            HavePrinter=true;
            mUsbManager.requestPermission(device, mPermissionIntent);
          }
        }
      }
      // Create the toast
      if (HavePrinter) {
        callbackContext.success("SUCCESS_CONNECT_USB");
        // Toast.makeText(cordova.getActivity(), "YEAY FOUND IT!", Toast.LENGTH_LONG).show();
      } else {
        callbackContext.error("FAILED_CONNECT_USB");
        // Toast.makeText(cordova.getActivity(), "TRY AGAIN, YOU CAN DO IT!", Toast.LENGTH_LONG).show();
      }
    } catch (Exception e) {
      Log.e("Print", (new StringBuilder("this --> connect ")).append(e.getMessage()).toString());
    }
  }
  
  private void printSample() {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Print.Initialize();
					PAct.BeforePrintAction();
					String ReceiptLines [] = {
            "      Electronics\n",
            "     Technology Co., Ltd.\n",
            "\n",
            "TEL +86-(0)592-5885991\n",
            "                             C#2\n",
            "         2013-08-20\n",
            "LPA                   $333.00\n",
            "--------------------------------\n",
            "LPB                  $444.00\n",
            "--------------------------------\n",
            "LPC                    $555.00\n",
            "--------------------------------\n",
            "\n",
            "Before adding tax       $1332.00\n",
            "tax   5.0%                $66.60\n",
            "--------------------------------\n",
            "total                   $1398.60\n",
            "Customer's payment      $1400.00\n",
            "Change                     $1.40\n"
          };
					for(int i=0;i<ReceiptLines.length;i++) {
            Log.e("DEBUG", ReceiptLines[i]);
            Print.PrintText(ReceiptLines[i]);
          }
          Log.e("DEBUG", "Finish After Loop");
					PAct.AfterPrintAction();
          Log.e("DEBUG", "Finish After Print Action");
				}
				catch(Exception e)
				{
					Log.e("Print", (new StringBuilder("Activity_Main --> PrintSampleReceipt ")).append(e.getMessage()).toString());
				}
			}
		});
	}

  private void customPrint(String message, CallbackContext callbackContext) {
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
          JSONObject obj = new JSONObject(message);
          JSONArray arr = obj.getJSONArray("data");
					// Print.Initialize();
					// PAct.BeforePrintAction();
					// Log.e("RESULT", message);
					for(int i=0;i<arr.length();i++) {
            String result = arr.getString(i);
            String[] split = result.split(";");
            Log.e("DATA", result);
            if (split.length == 4) {
              // PrintText(String data,int alignment,int attribute,int textSize)
              // 0 - 1
              // 0 - 15
              // 0 To 7, 16 To 23, 32 To 39, 48 To 55, 64 To 71, 80 To 87, 96 To 103, 112 To 119;
              int alignment = Integer.parseInt(split[1]); 
              int attribute = Integer.parseInt(split[2]); 
              int textSize = Integer.parseInt(split[3]);  
              Print.PrintText(split[0], alignment, attribute, textSize);
            } else if (split[0].startsWith("http")) {
                try {
                  URL url = new URL(split[0]);
                  HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                  connection.setDoInput(true);
                  connection.connect();
                  InputStream input = connection.getInputStream();
                  Bitmap myBitmap = BitmapFactory.decodeStream(input);
                  // - call printer
                  Print.PrintBitmap(myBitmap, 0, 0);
                } catch (IOException e) {
                    // Log exception
                    String errMsg = (new StringBuilder("Activity_Main --> PrintBitmapLZO ")).append(e.getMessage()).toString();
                    callbackContext.error("Failed when try print bitmap: " + errMsg);
                    Log.e("Print", errMsg);
                }
            } else {
              Print.PrintText(split[0]);
            }
          }
					// PAct.AfterPrintAction();
          callbackContext.success(message);
				}
				catch(Exception e)
				{
          String errMsg = (new StringBuilder("Activity_Main --> PrintSampleReceipt ")).append(e.getMessage()).toString();
          callbackContext.error("Failed when try custom print because: " + errMsg);
					Log.e("Print", errMsg);
				}
			}
		});
	}

  public void cutPaper() {
    try {
      Print.CutPaper(Print.PARTIAL_CUT_FEED, 240);
      Log.e("CUT-PAPER", "Succes cut paper!");
    } catch (Exception e) {
      Log.e("Print", (new StringBuilder("Activity_Main --> cutPaper ")).append(e.getMessage()).toString());
    }
  }

  public void isDeviceCompatible() {
    String msg = this.sensor != null ? "This device is compatible" : "Sorry, this device doesn't have a temperature sensor";

    Toast.makeText(cordova.getActivity(), msg, Toast.LENGTH_LONG).show();
    this.callbackContext.success(msg);
  }

  public void start () {
    if ((this.status == RUNNING) || (this.status == STARTING)) {
      return;
    }
    
    if(this.sensor != null) {
      this.sensorManager.registerListener(this, this.sensor, SensorManager.SENSOR_DELAY_NORMAL);
      this.lastAccessTime = System.currentTimeMillis();
      this.status = STARTING;
      Toast.makeText(cordova.getActivity(), "We're detecting the temperature of your surroundings...", Toast.LENGTH_LONG).show();

    } else {
        this.status = ERROR_FAILED_TO_START;
        this.stop();
        Toast.makeText(cordova.getActivity(), "Sorry this device does not have a Temperature sensor" , Toast.LENGTH_LONG).show();
        this.callbackContext.error("Sorry this device does not have a Temperature sensor");
    }
  }

  public void stop () {
    if (this.status != STOPPED) {
        this.sensorManager.unregisterListener(this);
    }
    this.status = STOPPED;
  }

  @Override
  public void onSensorChanged (SensorEvent sensorEvent) {
    try {
      if (sensorEvent.values.length > 0) {
        this.temperature = sensorEvent.values[0];
      }
      this.timeStamp = System.currentTimeMillis();
      this.status = RUNNING;

      if ((this.timeStamp - this.lastAccessTime) > this.TIMEOUT) {
          this.stop();
          callbackContext.error("Sorry there was a timeout, please try again.");
          return;
      }

      String msg = "Your current temperature is " + this.temperature;

      callbackContext.success(msg);
      Toast.makeText(cordova.getActivity(), msg , Toast.LENGTH_LONG).show();

    } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error("Sorry we encountered an issue. Please try again.");
    }
  }

  @Override
  public void onAccuracyChanged (Sensor sensor, int i) {

  }

  @Override
  public void onDestroy () {
      // this.stop();
  }

  @Override
  public void onReset () {
      // this.stop();
  }
}
