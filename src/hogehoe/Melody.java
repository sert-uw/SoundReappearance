package hogehoe;

public class Melody {
	//各音階の周波数
	public static final double C = 261.6255653005986;
	public static final double Db = 277.1826309768721;
	public static final double D = 293.6647679174076;
	public static final double Eb = 311.12698372208087;
	public static final double E = 329.6275569128699;
	public static final double F = 349.2282314330039;
	public static final double Gb = 369.9944227116344;
	public static final double G = 391.99543598174927;
	public static final double Ab = 415.3046975799451;
	public static final double A = 440;
	public static final double Bb = 466.1637615180899;
	public static final double B = 493.8833012561241;

	//48倍音までのデシベル値
	private static final int[] db = {
		-25, -33, -30, -44, -46, -66, -53, -45, -62, -65,
		-75, -64, -70, -68, -83, -69, -78, -85, -76, -81,
		-83, -87, -87, -86, -88, -87, -89, -90, -92, -117,
		-117, -117, -117, -117, -117, -117, -117, -117, -117, -117,
		-117, -117, -117, -117, -117, -117, -117, -117
	};

	private int octave;
	private double code;
	private double hz;
	private MakeSin[] sin = new MakeSin[48]; //48倍音までのsin波を保持

	private int count = 0;
	private int startTime; //演奏開始時間(ms)
	private double waitTime; //演奏開始まで待機中の経過時間
	private int playLength; //演奏時間(ms)
	private double playTime; //演奏開始してからの経過時間
	private boolean isPlaying;
	private double level;

	//コード文字列を受け取り周波数を返す
	public static double getCodeFromStr(String str){
		switch(str){
		case "C":
		case "ド":
			return C;

		case "Db":
		case "C#":
		case "レb":
		case "ド#":
			return Db;

		case "D":
		case "レ":
			return D;

		case "Eb":
		case "D#":
		case "ミb":
		case "レ#":
			return Eb;

		case "E":
		case "ミ":
			return E;

		case "F":
		case "ファ":
			return F;

		case "Gb":
		case "F#":
		case "ソb":
		case "ファ#":
			return Gb;

		case "G":
		case "ソ":
			return G;

		case "Ab":
		case "G#":
		case "ラb":
		case "ソ#":
			return Ab;

		case "A":
		case "ラ":
			return A;

		case "Bb":
		case "A#":
		case "シb":
		case "ラ#":
			return Bb;

		case "B":
		case "シ":
			return B;
		default:
			return -1;
		}
	}

	//コンストラクタ
	public Melody(int octave, double code, int playLength, int startTime, double level){
		if(octave < 0)
			octave = 0;
		this.octave = octave;
		this.code = code;
		this.playLength = playLength;
		this.startTime = startTime;
		this.level = level;

		//オクターブ値より基本周波数(1倍音)を求める
		double mag = 1 / 8.0;
		for(int i=0; i<octave; i++){
			mag *= 2;
		}

		hz = mag * code;

		//基本周波数(1倍音)から48倍音までのsin波をセットする
		for(int i=0; i<sin.length; i++){
			sin[i] = new MakeSin(hz * (i+1));
		}
	}

	public boolean isPlaying(){
		return isPlaying;
	}

	public double getSin(){
		//指定された演奏開始時間まで待機し、0を返す
		if(!isPlaying)
			waitTime += 1.0 / Main.SAMPLING * 1000.0;

		if(waitTime >= startTime){
			isPlaying = true;
		}else {
			return 0;
		}

		//演奏中のサンプリング回数をカウントし、時間を計測する
		count++;
		playTime += 1.0 / Main.SAMPLING * 1000.0;

		//指定された演奏時間を過ぎれば0を返して演奏を終了
		if(playTime >= playLength){
			isPlaying = false;
			return 0;
		}

		//48倍音までを指定されたデシベル値に基づいて重ね合わせる
		double sinSum = 0;
		for(int j=0; j<sin.length; j++){
			sinSum += calcMagFromDb(db[j]) * sin[j].getAmplitude();
		}

		//重ね合わせたデータの大きさを調整する
		double sinData = sinSum / sin.length * 476.62;
		//指定された音量レベルに調整する
		sinData *= level;

		//経過時間による音の減衰を行う
		double down = (Main.SAMPLING / 16) / (double)count;

		//上記反比例の値が1.0までとそれ以降で分岐
		if(down < 1.0){
			//反比例と下に凸な2次関数を用いて減衰率を定める
			if(count < Main.SAMPLING * 3)
				down = (down + 0.8 / (9.0 * Main.SAMPLING * Main.SAMPLING) * Math.pow(count - Main.SAMPLING*13/4, 2) + 0.1) / 2;
			//一定時間経過後は一定値を取る
			else
				down = (down + 0.1) / 2;
			sinData = (int)(sinData * down);
		}else {
			//反比例値が1.0になる直前に1次関数で瞬時に音量を上げる
			if(count > Main.SAMPLING*15/256)
				down = 256.0 / Main.SAMPLING * count - 15.0;
			//上記条件までは無音
			else
				down = 0;
			sinData = (int)(sinData * down);
		}

		//コードミスから生まれた謎の処理　何故かdownをもう一度掛けると減衰率が良い感じになる
		sinData *= down;

		return sinData;
	}

	public static double calcMagFromDb(int db){
		return (Math.pow(10, db / 20.0));
	}
}
