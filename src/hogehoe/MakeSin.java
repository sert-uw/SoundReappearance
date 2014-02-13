package hogehoe;

public class MakeSin {
	private double hz;
	private int count;

	public MakeSin(double hz){
		this.hz = hz;
		count = 0;
	}

	public double getAmplitude(){
		count++;

		return Math.pow(2, Main.BIT - 1) * Math.sin(count * hz * 2 * Math.PI / (double)Main.SAMPLING);

	}
}
