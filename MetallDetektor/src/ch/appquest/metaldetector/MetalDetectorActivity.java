package ch.appquest.metaldetector;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ProgressBar;

/**
 * Unsere Activity leitet von Activity ab um die Action Bar bereitzustellen und
 * implementiert zusätzlich das SensorEventListener Interface um über Sensor
 * Events benachrichtigt zu werden.
 * 
 * Siehe dazu auch Kapitel 15 des Buchs.
 */
public class MetalDetectorActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor magnetFieldSensor;
	private ProgressBar strengthIndicator;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		 * Zugriff auf die Sensoren erhalten wir über einen System Service,
		 * welcher uns einen handler vom Typ SensorManager liefert.
		 */
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		/*
		 * Da es mehrere Sensoren vom Typ TYPE_MAGNETIC_FIELD geben könnte
		 * wählen wir einfach den ersten aus, oder werfen eine Exception um die
		 * Anwendung zu unterbrechen.
		 */

		List<Sensor> magnetFieldSensors = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);

		if (magnetFieldSensors.isEmpty()) {
			throw new RuntimeException("No sensor of type TYPE_MAGNETIC_FIELD found.");
		}

		magnetFieldSensor = magnetFieldSensors.get(0);

		strengthIndicator = (ProgressBar) findViewById(R.id.progressBar);
		strengthIndicator.setMax((int) magnetFieldSensor.getMaximumRange());
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		/*
		 * Wir sind an diesen Updates nicht interessiert, da wir sowieso keine
		 * genauen Messwerte des Sensors brauchen.
		 */
	}

	@Override
	protected void onResume() {
		super.onResume();

		/*
		 * Wenn der magnetFieldSensor gefunden wurde, melden wir uns für Updates
		 * an.
		 */
		if (magnetFieldSensor != null) {
			sensorManager.registerListener(this, magnetFieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		/*
		 * Um Resourcen zu sparen melden wir uns beim verlassen der Activity
		 * wieder vom Sensor Manager ab.
		 */
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		/*
		 * Zuerst prüfen wir, ob der Event auch wirklich unseren
		 * magnetFieldSensor betrifft. Falls wir mehrere Sensoren verwenden
		 * können wir hier unterscheiden welcher Sensor die Messwerte liefert.
		 */
		if (event.sensor == magnetFieldSensor) {
			/*
			 * Dieser Sensor liefert uns gleich 3 Werte, und zwar einen Wert für
			 * jede der 3 Achsen, also im Prinzip ein 3D-Vektor. Um einen
			 * absoluten Wert für die Darstellung zu erhalten, berechnen wir
			 * einfach den Betrag (die Länge) des Vektors.
			 */
			float[] mag = event.values;
			double abs = android.util.FloatMath.sqrt(mag[0] * mag[0] + mag[1] * mag[1] + mag[2] * mag[2]);

			strengthIndicator.setProgress((int) abs);
		}
	}

	private static final int SCAN_QR_CODE_REQUEST_CODE = 0;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.add("Log");
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, SCAN_QR_CODE_REQUEST_CODE);
				return false;
			}
		});
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SCAN_QR_CODE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				log(intent.getStringExtra("SCAN_RESULT"));
			}
		}
	}

	private void log(String qrCode) {
		Intent intent = new Intent("ch.appquest.intent.LOG");
		intent.putExtra("ch.appquest.taskname", "Metall Detektor");
		intent.putExtra("ch.appquest.logmessage", qrCode);
		startActivity(intent);
	}
}
