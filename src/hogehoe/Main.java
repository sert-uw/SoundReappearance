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

		List<Melody> melodys = new ArrayList<Melody>();

		try{
			BufferedReader br = new BufferedReader(new FileReader(new File("./" + fileName + ".dat")));

			String str;
			int beforeTime = 0;
			double level = 1.0;
			double addLevel = 0;
			while((str = br.readLine()) != null){
				if(str.equals("reset")){
					beforeTime = 0;
					continue;
				}

				if(str.indexOf("level") != -1){
					StringTokenizer st = new StringTokenizer(str, ",");
					st.nextToken();
					level = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					double toLevel = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					double div = Double.parseDouble(st.nextToken().replaceAll("[^0-9]", ""));
					addLevel = (toLevel - level) / div;
					continue;
				}

				StringTokenizer st = new StringTokenizer(str, ",");
				if(st.countTokens() < 3)
					continue;

				int octave = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));
				String codeStr = st.nextToken().replaceAll(" ", "");
				int length = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));
				int startTime = Integer.parseInt(st.nextToken().replaceAll("[^0-9]", ""));

				if(startTime != 0)
					level += addLevel;

				double code = Melody.getCodeFromStr(codeStr);
				if(code == -1)
					continue;

				melodys.add(new Melody(octave, code, length, beforeTime + startTime, level));
				beforeTime += startTime;
			}

			br.close();
		}catch (IOException e){
			e.printStackTrace();
		}

		double max = 0;
		for(int i=0; i<sinData.length; i++){
			double sinSum = 0;
			for(Melody melody : melodys){

				sinSum += melody.getSin();
			}

			sinData[i] = (int)sinSum;

			if(Math.abs(sinData[i]) > max)
				max = Math.abs(sinData[i]);
		}

		for(int i=0; i<sinData.length; i++){
			sinData[i] = (int)(sinData[i] * (((Math.pow(2, 15) * 0.9) / max)));

			data[2 * i] = (byte)(sinData[i] & 0x0000ffff);
			data[2 * i + 1] = (byte)(sinData[i] / (16 * 16));
		}

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
