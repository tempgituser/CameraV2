package af.camerav2;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.util.*;
import android.hardware.camera2.*;
import android.media.*;
import android.view.TextureView.*;
import android.graphics.*;
import android.content.*;
import java.util.*;
import android.hardware.camera2.params.*;
import java.nio.*;
import java.io.*;
import java.lang.Process;
import android.content.res.*;

public class MainActivity extends Activity implements View.OnClickListener
{
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	static{
		ORIENTATIONS.append(Surface.ROTATION_0, 90);
		ORIENTATIONS.append(Surface.ROTATION_90, 0);
		ORIENTATIONS.append(Surface.ROTATION_180, 270);
		ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}	
	private AutoFitTextureView textureView;
	private String mCameraId = "0";//0:back,1:front
	private CameraDevice cameraDevice;
	private Size previewSize;//preview size
	private CaptureRequest.Builder previewRequestBuilder;
	private CaptureRequest previewRequest;
	private CameraCaptureSession captureSession;
	private ImageReader imageReader;
	private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener(){
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height)
		{
			//width=720;
			//height=1080;
			showToast(""+width+","+height);
			openCamera(1080, 1920);
		}
		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height)
		{

		}
		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture texture)
		{
			return true;
		}
		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture texture)
		{

		}
	};
	private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback(){
		@Override
		public void onOpened(CameraDevice cameraDevice)
		{
			MainActivity.this.cameraDevice = cameraDevice;
			createCameraPreviewSession();
		}
		@Override
		public void onDisconnected(CameraDevice cameraDevice)
		{
			cameraDevice.close();
			MainActivity.this.cameraDevice = null;
			isShutting = false;
		}
		@Override
		public void onError(CameraDevice cameraDevice, int error)
		{
			cameraDevice.close();
			MainActivity.this.cameraDevice = null;
			Toast t = Toast.makeText(MainActivity.this, "errorCode:" + error, 1);
			t.show();
			isShutting = false;
			MainActivity.this.finish();
		}
	};

	public boolean isShutting = false;
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		
		//WindowManager.LayoutParams lp = this.getWindow().getAttributes();
		////0到1,调整亮度暗到全亮
		//lp.screenBrightness = Float.valueOf(0/255f); 
		//this.getWindow().setAttributes(lp);
		//execShellCmdRoot("echo 0 > /sys/class/leds/lcd-backlight/brightness");

		//showWhiteView();

		//Camera
		//textureView = (AutoFitTextureView)findViewById(R.id.texture);
		//textureView.setSurfaceTextureListener(mSurfaceTextureListener);
		findViewById(R.id.capture).setOnClickListener(this);


		Handler h = new Handler(){
			@Override
			public void handleMessage(Message msg){

				captureStillPicture();
			}
		};

		//h.sendEmptyMessageDelayed(0,3000);
    }
	
	boolean isDebug = true;
	Toast keyToast;
	public void showToast(String s){
		if(!isDebug){return;}
		if (keyToast != null){
			//keyToast.cancel();
		}
		keyToast = Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT);
		keyToast.show();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		
		if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
			showToast("dispatch");
			vib(50);
			captureStillPicture();
			return true;
		}
		
		// TODO: Implement this method
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
			showToast("keydown");//vib(50);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
			showToast("keyup");
			//vib(50);
			return true;

		}
		
		// TODO: Implement this method
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return false;
	} 
	
	
	@Override
	protected void onDestroy()
	{

		showWhiteView();
		
		// TODO: Implement this method
		super.onDestroy();
		if(cameraDevice != null){
			cameraDevice.close();
			MainActivity.this.cameraDevice = null;
		}
		this.finish();
		System.exit(0);
	}

	@Override
	protected void onStop()
	{
		textureView = (AutoFitTextureView)findViewById(R.id.texture);
		textureView.setSurfaceTextureListener(null);
		if(cameraDevice != null){
			MainActivity.this.cameraDevice = null;
			cameraDevice.close();
		}
		// TODO: Implement this method

		showWhiteView();
		super.onStop();

		this.finish();
		System.exit(0);
	}

	@Override
	protected void onResume()
	{
		// TODO: Implement this method
		super.onResume();

		textureView = (AutoFitTextureView)findViewById(R.id.texture);
		textureView.setSurfaceTextureListener(mSurfaceTextureListener);
	}

	@Override
	protected void onStart()
	{
		// TODO: Implement this method
		super.onStart();
	}

	@Override
	protected void onPause()
	{
		// TODO: Implement this method

		textureView = (AutoFitTextureView)findViewById(R.id.texture);
		textureView.setSurfaceTextureListener(null);
		
		if(cameraDevice != null){
			cameraDevice.close();
			MainActivity.this.cameraDevice = null;
		}

		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		
		showWhiteView();
		super.onPause();

		this.finish();
		System.exit(0);
		
	}
	
	public void showWhiteView(){
		findViewById(R.id.texture).setVisibility(View.GONE);
		findViewById(R.id.white).setVisibility(View.VISIBLE);
	}
	
	@Override
	public void onClick(View p1)
	{
		captureStillPicture();
	}

	private void captureStillPicture()
	{
		if(isShutting){
			return;
		}
		isShutting = true;
		try
		{
			if (cameraDevice == null)
			{
				Toast.makeText(MainActivity.this, "mCamera == null!", Toast.LENGTH_SHORT).show();
				isShutting = false;
				return;
			}
			
			try{Thread.sleep(2000);}
			catch(Exception e){}
			
			final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureRequestBuilder.addTarget(imageReader.getSurface());
			captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);//set to auto focus mode
			captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
			int rotation = getWindowManager().getDefaultDisplay().getRotation();
			captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
			captureSession.stopRepeating();

			captureSession.capture(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback(){
					@Override
					public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
					{
						try
						{
							previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
							previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
							captureSession.setRepeatingRequest(previewRequest, null, null);

						}
						catch (CameraAccessException e)
						{
							e.printStackTrace();
						}
					}
				}, null);

		}
		catch (CameraAccessException e)
		{
			e.printStackTrace();
		}

	}

	private void openCamera(int width, int height)
	{
		setUpCameraOutputs(width, height);
		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		try
		{
			manager.openCamera(mCameraId, stateCallback, null);
		}
		catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
	}
	private void createCameraPreviewSession()
	{
		try
		{
			SurfaceTexture texture = textureView.getSurfaceTexture();
			texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());


			previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			Surface surface = new Surface(texture);
			previewRequestBuilder.addTarget(surface);
			cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback(){
				@Override
				public void onConfigured(CameraCaptureSession cameraCaptureSession){
					if(cameraDevice == null){
						return;
					}
					captureSession = cameraCaptureSession;
					try{
						previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
						previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
						previewRequest = previewRequestBuilder.build();
						captureSession.setRepeatingRequest(previewRequest, null, null);
					}catch(CameraAccessException e){
						e.printStackTrace();
					}
				}
				@Override
				public void onConfigureFailed(CameraCaptureSession cameracaptureSession){
					Toast.makeText(MainActivity.this, "configure fault!", Toast.LENGTH_SHORT).show();
				}
				
			}, null);
			
		}
		catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
	}
	
	private void setUpCameraOutputs(int width,int height){
		CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
		try{
			CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			
			Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
			showToast("largestWidth:"+largest.getWidth()+"  largestHeight:"+largest.getHeight());
			
			imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),ImageFormat.JPEG, 2);
			imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener(){
				@Override
				public void onImageAvailable(ImageReader reader){
					Image image = reader.acquireNextImage();
					ByteBuffer buffer = image.getPlanes()[0].getBuffer();
					byte[] bytes = new byte[buffer.remaining()];
					Date date = new Date();
					
					
					
					File noMedia = new File(getExternalFilesDir(null),".nomedia");
					if(!noMedia.exists()){
						try
						{
							noMedia.createNewFile();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					
					
					File file = new File(getExternalFilesDir(null),"pic"+date.getTime()+".jpg");
					buffer.get(bytes);
					try{
						FileOutputStream output = new FileOutputStream(file);
						Toast.makeText(MainActivity.this, "Saved:"+file, Toast.LENGTH_SHORT).show();
						output.write(bytes);
						vib(50);

						isShutting = false;
					}catch(Exception ex){
						
					}finally{
						image.close();
					}
				}
			}, null);
			
			previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),width, height, largest);
			int orientation = getResources().getConfiguration().orientation;
			if(orientation == Configuration.ORIENTATION_LANDSCAPE){
				textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
			}else{
				textureView.setAspectRatio(previewSize.getHeight(),previewSize.getWidth());
			}
			
		}catch(CameraAccessException e){
			e.printStackTrace();
		}catch(NullPointerException e){
			e.printStackTrace();
		}
	}
	
	private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio){
		
		//width=720;
		//ight=1280;
		List<Size>bigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for(Size option:choices){
			
			if(option.getHeight()==option.getWidth()*h/w && option.getWidth() >= width && option.getHeight() >= height){
				bigEnough.add(option);
			}
		}
		if(bigEnough.size() > 0){

			Size good = Collections.min(bigEnough, new CompareSizesByArea());
			showToast("chosenWidth:"+good.getWidth()+"  chosezHeight:"+good.getHeight());
			
			return Collections.min(bigEnough, new CompareSizesByArea());
		}else{
			showToast("can't find fit previewSize!");
			System.out.println("can't find fit previewSize!");
			return choices[0];
		}
	}
	
	public void vib(int millsec){
		Vibrator vibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
		vibrator.vibrate(millsec);
	}
	
	
	
	
	
	
	private void execShellCmdRoot(String cmd) {
		try {
			Process process = Runtime.getRuntime().exec("su");
			// ��ȡ�����
			OutputStream outputStream = process.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeBytes(cmd);
			dataOutputStream.flush();
			dataOutputStream.close();
			outputStream.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	@SuppressWarnings("unused")
	private void execShellCmd(String cmd) {
		try {
			Process process = Runtime.getRuntime().exec("");
			// ��ȡ�����
			OutputStream outputStream = process.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			dataOutputStream.writeBytes(cmd);
			dataOutputStream.flush();
			dataOutputStream.close();
			outputStream.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void execShellCmds(String[] cmds) {
		try {
			Process process = Runtime.getRuntime().exec("su");
			// ��ȡ�����
			OutputStream outputStream = process.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
			for (String s : cmds) {
				dataOutputStream.writeBytes(s);
			}
			dataOutputStream.flush();
			dataOutputStream.close();
			outputStream.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	
	
	
	
	static class CompareSizesByArea implements Comparator<Size>{
		@Override
		public int compare(Size lhs, Size rhs){
			return Long.signum((long)lhs.getWidth()*lhs.getHeight()-(long)rhs.getWidth()*rhs.getHeight());
		}
	}
}
