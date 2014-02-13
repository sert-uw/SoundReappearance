package hogehoe;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class Main {
	private static final int SEC = 80; //wavファイルに書き込む音楽の長さ
	public static final int SAMPLING = 44100; //サンプリングレート
	public static final int BIT = 16;; //１サンプリングごとのデータの大きさ

	private static final String fileName = "Fighting"; //Input,Output用のファイル名

	public static void main(String[] args) {
		byte[] data = new byte[SEC * SAMPLING * (BIT / 8)]; //ファイルに書き込むバイトデータ
		int[] sinData = new int[SEC * SAMPLING]; //ファイルに書き込む最終的な値

		List<Melody> melodys = new ArrayList<Melody>(); //奏でる音階のリスト

		try{
			//データファイルの読み込み
			BufferedReader br = new BufferedReader(new FileReader(new File("./" + fileName + ".dat")));

			String str;
			int beforeTime = 0; //1つ前のデータの開始時間
			double level = 1.0; //音量レベル
			double addLevel = 0; //各データごとの音量レベルの変化量(クレッシェンド、デクレッシェンド用)

			while((str = br.readLine()) != null){
				//譜面の上段を一通り取得したら下段を取得するために時間をリセットする
				if(str.equals("reset")){
					beforeTime = 0;
					continue;
				}

				//levelの文字列があれば音量レベルを設定する
				if(str.indexOf("level") != -1){
					StringTokenizer st = new StringTokenizer(str, ",");
					st.nextToken();
					//瞬時に変更するレベル値
					level = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					//徐々に変化させる大きさの基準点
					double toLevel = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					//上記２つのレベルを何分割するか
					double div = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					//変化量を決める
					addLevel = (toLevel - level) / div;
					continue;
				}

				//データを","で区切り、その要素が3より少なければ改行などとみなす
				StringTokenizer st = new StringTokenizer(str, ",");
				if(st.countTokens() < 3)
					continue;

				//音階のオクターブ値
				int octave = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));
				//音階の文字列を取得
				String codeStr = st.nextToken().replaceAll(" ", "");
				//音を鳴らす長さを取得
				int length = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));
				//１つ前のデータとの時間差を取得
				int startTime = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));

				//１つ前のデータと同時に鳴らさない場合のみ音量レベルを変化させる
				if(startTime != 0)
					level += addLevel;

				//音階の文字列に対応する周波数値を取得
				double code = Melody.getCodeFromStr(codeStr);
				if(code == -1)
					continue;

				//音を設定する
				melodys.add(new Melody(octave, code, length, beforeTime + startTime, level));
				//開始時間を更新する
				beforeTime += startTime;
			}

			br.close();
		}catch (IOException e){
			e.printStackTrace();
		}

		double max = 0; //各音を合成した後の、サンプリングの最大値
		for(int i=0; i<sinData.length; i++){
			double sinSum = 0;
			//各音を重ね合わせる
			for(Melody melody : melodys){
				sinSum += melody.getSin();
			}

			//Outputようにint型に変換する
			sinData[i] = (int)sinSum;

			if(Math.abs(sinData[i]) > max)
				max = Math.abs(sinData[i]);
		}

		//各サンプリング値を修正する
		for(int i=0; i<sinData.length; i++){
			//サンプリングの最大値がwav形式の上限の0.9になるように全体の大きさを調整
			sinData[i] = (int)(sinData[i] * (((Math.pow(2, 15) * 0.9) / max)));

			//リトルエンディアンで書き込みを行うようにint型をbyte型に分割
			data[2 * i] = (byte)(sinData[i] & 0x0000ffff);
			data[2 * i + 1] = (byte)(sinData[i] / (16 * 16));
		}

		//以下wavファイルへの書き込み
		AudioFormat format = new AudioFormat(SAMPLING, BIT, 1, true, false);
		AudioInputStream ais = new AudioInputStream(
				new ByteArrayInputStream(data), format, data.length/2);

		try{
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("./" + fileName + ".wav"));
		}catch (IOException e){
			e.printStackTrace();
		}

		System.out.println("Writing success!");
	}
}
