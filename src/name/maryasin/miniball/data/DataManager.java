package name.maryasin.miniball.data;

import java.io.*;
import java.util.*;

import android.net.Uri;
import android.util.Log;

import name.maryasin.miniball.util.Settings;

/**
 * This static class manages Dances and Materials.
 */
public class DataManager {
	/** For logs only */
	public static final String TAG = "DataManager";
	
	/** Called whenever DataManager decides that data changed and must be reloaded */
	public interface DataChangedListener {
		public void onDataChanged();
	}
	private static List<DataChangedListener> listeners = new ArrayList<DataChangedListener>();
	public static void addDataChangedListener(DataChangedListener listener) {
		listeners.add(listener);
	}
	public static void removeDataChangedListener(DataChangedListener listener) {
		listeners.remove(listener);
	}
	private static void fireDataChanged() {
		Log.d(TAG, "Firing onDataChanged");
		for(DataChangedListener l: listeners) {
			Log.d(TAG, "firing: "+l);
			l.onDataChanged();
		}
	}
	
	/**
	 * Path to root directory of database
	 */
	public static File rootPath;

	//	public static List<Dance> ITEMS = new ArrayList<Dance>();

	/** Карта, соотносящая все Танцы с их именами */
	public static Map<String, Dance> danceMap;
	/** Карта, соотносящая имена не только с Танцами, но и с Псевдонимами */
	public static Map<String, Alias> aliasMap;

	public static boolean isDanceListInitialized() {
		return danceMap != null && aliasMap != null;
	}
	public static void initDanceList() throws IOException {
		rootPath = new File(Settings.get().getRootPath());
		
		try {
			if(!rootPath.isDirectory())
				throw new IOException("Указанный корневой каталог "+rootPath+" не является каталогом!");
			
			Log.d(TAG, "Загружаем танцы");
			danceMap = new TreeMap<String, Dance>(String.CASE_INSENSITIVE_ORDER);
			aliasMap = new TreeMap<String, Alias>(String.CASE_INSENSITIVE_ORDER);
			for(File f: rootPath.listFiles())
				if(f.isDirectory() && // рассматриваем только папки,
						new File(f, "_alias").exists()) { // в которых есть псевдонимы
					Dance d = new Dance(f.getName());
					danceMap.put(d.getName(), d);
					aliasMap.put(d.getName(), d);
				}

			Log.d(TAG, "Загружаем псевдонимы");
			// Грузим псевдонимы для всех танцев
			for(Dance d: danceMap.values()) {
				d.initAliases(); // отдельной операцией - для возможного отображения прогресса
				for(String a: d.getAliases())
					if(!a.equalsIgnoreCase(d.getName())) // игнорируем псевдонимы, одноимённые самому танцу
						if(aliasMap.containsKey(a))
							aliasMap.get(a).addRef(d);
						else
							aliasMap.put(a, new Alias(a, d));
			}
			// закрываем списки
			danceMap = Collections.unmodifiableMap(danceMap);
			aliasMap = Collections.unmodifiableMap(aliasMap);
			fireDataChanged();
			Log.d(TAG, "Танцы и псевдонимы загружены");
		} catch (IOException e) {
			// заметаем следы, чтобы не было лишних exceptions при отображении чего попало
			// TODO: может, лучше работать с тем, что удалось загрузить? Возможно, загружать всё что только удастся?
			danceMap = Collections.emptyMap();
			aliasMap = Collections.emptyMap();
			fireDataChanged(); // it is a change too...
			throw e;
		}
	}

	private static Map<Query, Set<Dance>> danceSearchCache = new HashMap<Query, Set<Dance>>();
	/** Возвращает перечень танцев, соответствующих запросу */
	public static Set<Dance> findDances(Query q) {
		if(!isDanceListInitialized())
			throw new IllegalStateException("DataManager не инициализирован");
		
		if(danceSearchCache.containsKey(q)) // если по этому запросу уже есть ответ
			return danceSearchCache.get(q);
		Set<Dance> ret = new HashSet<Dance>();
		for(Dance d: danceMap.values())
			if(q.match(d))
				ret.add(d);
		danceSearchCache.put(q, ret); // запоминаем результат на будущее
		return ret;
	}
	private static Map<Query, List<Alias>> aliasSearchCache = new HashMap<Query, List<Alias>>();
	/** Возвращает отсортированный список Танцев, соответствующих запросу,
		состоящему из 0 или более имён псевдонимов,
		и Псевдонимов, которые есть у этих танцев */
	public static List<Alias> findAliases(Query q) {
		if(aliasSearchCache.containsKey(q)) // если по этому запросу уже есть ответ
			return aliasSearchCache.get(q);
		Set<Dance> dset = findDances(q);
		Set<Alias> aset = new HashSet<Alias>();
		aset.addAll(dset); // сначала добавим танцы
		for(Dance d: dset)
			for(String a: d.getAliases())
				aset.add(aliasMap.get(a)); // берём из map, чтобы сохранить refcount
		List<Alias> ret = new ArrayList<Alias>();
		ret.addAll(aset);
		Collections.sort(ret);
		aliasSearchCache.put(q, ret); // запоминаем результат на будущее
		return ret;
	}

	//////////////////
	// URI handling //

	private static boolean validateUri(Uri uri) {
		if(uri.getScheme() != "miniball")
			return false;
		return true;
	}

	/**
	 * Returns dance for given URI.
	 * Uri itself may link to material or alias.
	 * @param uri
	 * @return
	 */
	public static Dance getDanceFromUri(Uri uri)
			throws IllegalArgumentException {
		if(!validateUri(uri))
			throw new IllegalArgumentException("Bad uri: " + uri);
		String name = uri.getPathSegments().get(0);
		if(!aliasMap.containsKey(name))
			throw new IllegalArgumentException("No such dance or alias: " + name);
		return aliasMap.get(name).getReferringDance();
	}

	public static Material getMaterialFromUri(Uri uri)
			throws IllegalArgumentException {
		List<String> ps = uri.getPathSegments();
		if(ps.size() != 2)
			throw new IllegalArgumentException("Not a material uri: " + uri);
		String mname = ps.get(1);
		Dance d = getDanceFromUri(uri);
		if(!d.getMaterials().containsKey(mname))
			throw new IllegalArgumentException("No such material: " + mname + " in dance "+d);
		return d.getMaterials().get(mname);
	}
}
