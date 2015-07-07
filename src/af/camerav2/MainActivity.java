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
			openCamera(width, height);
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
		}
		@Override
		public void onError(CameraDevice cameraDevice, int error)
		{
			cameraDevice.close();
			MainActivity.this.cameraDevice = null;
			Toast t = Toast.makeText(MainActivity.this, "errorCode:" + error, 1);
			t.show();
			MainActivity.this.finish();
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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

	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		
		if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN){
			//Toast.makeText(this,"downnnnnnnnnnnnnnnnnn",Toast.LENGTH_LONG).show();
			vib(50);
			
		}
		
		// TODO: Implement this method
		return super.dispatchKeyEvent(event);
	} 
	
	@Override
	protected void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		this.finish();
		System.exit(0);
	}

	@Override
	protected void onStop()
	{
		textureView = (AutoFitTextureView)findViewById(R.id.texture);
		textureView.setSurfaceTextureListener(null);
		// TODO: Implement this method
		super.onStop();
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
		

		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		
		super.onPause();

		
	}
	
	@Override
	public void onClick(View p1)
	{
		captureStillPicture();
	}

	private void captureStillPicture()
	{
		try
		{
			if (cameraDevice == null)
			{
				Toast.makeText(MainActivity.this, "mCamera == null!", Toast.LENGTH_SHORT).show();
				return;
			}
			final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureRequestBuilder.addTarget(imageReader.getSurface());
			captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);//set to auto focus mode
			captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
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
							previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
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
						previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
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
			imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),ImageFormat.JPEG, 2);
			imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener(){
				@Override
				public void onImageAvailable(ImageReader reader){
					Image image = reader.acquireNextImage();
					ByteBuffer buffer = image.getPlanes()[0].getBuffer();
					byte[] bytes = new byte[buffer.remaining()];
					Date date = new Date();
					
					File file = new File(getExternalFilesDir(null),"pic"+date.getTime()+".jpg");
					buffer.get(bytes);
					try{
						FileOutputStream output = new FileOutputStream(file);
						Toast.makeText(MainActivity.this, "Saved:"+file, Toast.LENGTH_SHORT).show();
						output.write(bytes);
						vib(100);
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
	
	private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio){
		List<Size>bigEnough = new ArrayList<>();
		int w = aspectRatio.getWidth();
		int h = aspectRatio.getHeight();
		for(Size option:choices){
			if(option.getHeight()==option.getWidth()*h/w && option.getWidth() >= width && option.getHeight() >= height){
				bigEnough.add(option);
			}
		}
		if(bigEnough.size() > 0){
			return Collections.min(bigEnough, new CompareSizesByArea());
		}else{
			System.out.println("can't find fit previewSize!");
			return choices[0];
		}
	}
	
	public void vib(int millsec){
		Vibrator vibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
		vibrator.vibrate(millsec);
	}
	
	
	
	
	
	
	static class CompareSizesByArea implements Comparator<Size>{
		@Override
		public int compare(Size lhs, Size rhs){
			return Long.signum((long)lhs.getWidth()*lhs.getHeight()-(long)rhs.getWidth()*rhs.getHeight());
		}
	}
}
