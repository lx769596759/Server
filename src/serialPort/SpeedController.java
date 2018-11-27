package serialPort;

public class SpeedController extends Thread {
	
	private String speed;

	public SpeedController(String speed) {
		this.speed = speed;
		this.start();
	}

	@Override
	public void run() {
		double lineSpeed = Double.valueOf(speed);
		GetSpeed gs = new GetSpeed();
		gs.controlLidar(lineSpeed);
		gs = null;
	}

}
