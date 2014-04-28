package name.maryasin.miniball.data;

import java.io.*;
import java.util.*;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

/**
 * Материал танца. Пока что поддерживаются только mp3 файлы.
 * TODO: Может включать музыку, текст, html, видео и теги.
 */
public class Material implements Comparable<Material> {
	public Dance dance;
	public String name;
	/*package*/ File audioFile;
	private Set<String> tags;
	
	/*package*/ Material(Dance dance, String name) {
		this.dance = dance;
		this.name = name;
		tags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	}
	
	public boolean hasAudio() {
		return audioFile != null;
	}
	public File getAudioFile() {
		return audioFile;
	}

	public boolean hasImage() { return false; } // TODO
	public File getImageFile() { return null; } // TODO
	
	/*package*/ void addTag(String tag) {
		tags.add(tag);
	}
	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	public Uri getUri() {
		return Uri.withAppendedPath(dance.getUri(), name);
	}
	
	@Override
	public String toString() {
		return dance+": "+name
				+(hasAudio()?" [a]":"")
				+"\n  Tags: "+tags;
	}
	@Override
	public int compareTo(Material other) {
		return name.compareToIgnoreCase(other.name);
	}
}

