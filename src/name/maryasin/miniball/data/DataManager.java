package name.maryasin.miniball.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.os.Environment;
import android.util.Log;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DataManager {
	/** Только для логов */
	public static final String TAG = "DataManager";
	
	/**
	 * Path to root directory of database
	 */
	public static File rootPath = new File(
			Environment.getExternalStorageDirectory(),
			"MiniBall"); // TODO: configurable root path

//	public static List<Dance> ITEMS = new ArrayList<Dance>();

	/** Карта, соотносящая все Танцы с их именами */
	public static Map<String, Dance> danceMap;
	/** Карта, соотносящая имена не только с Танцами, но и с Псевдонимами */
	public static Map<String, Alias> aliasMap;

	public static boolean isDanceListInitialized() {
		return danceMap != null && aliasMap != null;
	}
	public static void initDanceList() throws IOException {
		if(!rootPath.isDirectory())
			throw new IOException("Указанный корневой каталог "+rootPath+" не является каталогом!");
		
		try {
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
							aliasMap.put(a, new Alias(a));
			}
			// закрываем списки
			danceMap = Collections.unmodifiableMap(danceMap);
			aliasMap = Collections.unmodifiableMap(aliasMap);
			Log.d(TAG, "Танцы и псевдонимы загружены");
		} catch (IOException e) {
			// заметаем следы, чтобы не было лишних exceptions при отображении чего попало
			// TODO: может, лучше работать с тем, что удалось загрузить? Возможно, загружать всё что только удастся?
			danceMap = Collections.emptyMap();
			aliasMap = Collections.emptyMap();
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

	/** Этот класс представляет псевдоним танца или танец (который является его субклассом).
		Использоваться данный класс должен только в общем перечне
		псевдонимов и танцев,
		в остальных случаях псевдоним представлен строкой.
	 */
	public static class Alias implements Comparable<Alias> {
		private String name;
		/** Число ссылок. Для псевдонима изначально 0, для танца 1 (он сам). */
		protected int refCount;
		
		/*package*/ Alias(String name) {
			this.name = name;
			refCount = 0; // изначально псевдоним ни на что не ссылается
		}
		
		/** Возвращает название данного элемента */
		public String getName() {
			return name;
		}

		/** Вызывается только при загрузке списка танцев и псевдонимов.
		 * Добавляет информацию о том, что на это псевдоним ссылается какой-либо танец.
		 * Танец пока не сохраняется, передаём для проформы.
		 * TODO: Возможно, сохранять для оптимизации getReferringDances()? */
		/*package-local*/ void addRef(Dance d) {
			refCount++;
		}
		/** Возвращает число танцев, использующих этот псевдоним */
		public int getRefCount() {
			return refCount;
		}
		/**
		 * Возвращает все танцы, использующие этот псевдоним.
		 */
		public Set<Dance> getReferringDances() {
			// TODO
			// TODO: кэширование?
			return null;
		}
		/** Возвращает танец (единственный), ссылающийся на этот псевдоним.
		 * Если таких танцев несколько, вызывает IllegalStateError */
		public Dance getReferringDance() {
			if(refCount > 1)
				throw new IllegalStateException("На псевдоним "+getName()+" слишком много сслыок: "+refCount);
			// TODO
			return null;
		}
		
		@Override
		public int compareTo(Alias another) {
			return name.compareToIgnoreCase(another.name);
		}
		@Override
		public boolean equals(Object o) {
			return (o instanceof Alias) &&
					((Alias)o).name.equalsIgnoreCase(name);
		}
		/** Танцы/псевдонимы считаются одинаковыми, если совпадает имя. */
		@Override
		public int hashCode() {
			return name.toLowerCase(Locale.getDefault()).hashCode();
			// toLowerCase - во избежание косяков с регистром, когда два танца равны по compareTo, но имеют разные хэши
		}

		@Override
		public String toString() {
			return name;
		}
	}
	/**
	 * Этот класс представляет один танец из базы данных
	 */
	public static class Dance extends Alias {
		private File danceRoot;
		private Set<String> aliases;
		private Map<String, Material> materials;

		/** После инициализации обязательно вызвать loadAliases() */
		/*package*/ Dance(String name) {
			super(name);
			refCount = 1; // любой танец ссылается сам на себя, как минимум
			danceRoot = new File(rootPath, name);
		}
		/** Необходимое дополнение к инициализации конструктором!
		 * Загружает информацию о псевдонимах танца (набор строк),
		 * сохраняет её в полях объекта. */
		public void initAliases() throws IOException {
			// файл должен существовать - проверяли при загрузке списка танцев
			FileInputStream fin = new FileInputStream(new File(
					danceRoot, "_alias"));
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(fin));
				try {
					aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
					String line;
					while ((line = r.readLine()) != null) {
						line = line.trim(); // на всякий случай убираем лишние пробелы на краях
						if(line.length() > 0 && // игнорируем пустые строки
								!line.startsWith("#") && // игнорируем комментарии
								!line.equalsIgnoreCase(getName())) // игнорируем псевдоним, одноимённый танцу
							aliases.add(line);
					}
				} finally {
					r.close();
				}
			} finally {
				fin.close();
			}
		}
		/**
		 * Возвращает Set псевдонимов танца. Проверяет, загружены ли уже псевдонимы.
		 * @throws IllegalStateException если псевдонимы не были загружены (т.е. танец не инициализирован как положено)
		 * @return
		 */
		public Set<String> getAliases() {
			if(aliases == null) {
				Log.e(TAG+".Dance", "Внимание: запрос к getAliases до загрузки псевдонимов");
				throw new IllegalStateException("Внимание: запрос к getAliases до загрузки псевдонимов");
			}
			return aliases;
		}
		
		public boolean areMaterialsInitialized() {
			return materials != null;
		}
		/** Инициализация перечня материалов данного танца. */
		public void initMaterials() throws IOException {
			if(!danceRoot.isDirectory())
				throw new IOException("Dance path "+danceRoot+" is not a directory!");
			
			materials = new TreeMap<String, Material>(String.CASE_INSENSITIVE_ORDER);
			for(File f: danceRoot.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					filename = filename.toLowerCase(Locale.getDefault());
					if(filename == "_alias") // игнорируем служебный файл
						return false;
					if(filename.endsWith(".mp3")) // TODO: другие типы
							return true;
					return false;
				}
			})) {
				String mname = f.getName();
				String lname = mname.toLowerCase(Locale.getDefault());
				
				mname = mname.substring(0, mname.lastIndexOf('.')).trim();
				Material m;
				if(materials.containsKey(mname))
					m = materials.get(mname);
				else {
					m = new Material(this, mname);
					materials.put(mname, m);
				}
				if(lname.endsWith(".mp3") || lname.endsWith(".ogg")) { // audio
					if(m.hasAudio())
						Log.w(TAG+".Dance", this+": несколько аудиофайлов к одному материалу: "
								+ m.getAudioFile() + ", " + f);
					else
						m.audioFile = f;
				} else if(lname.endsWith(".tag")) {
					// ...
				} else
					throw new InternalError("DataManager.Dance.initMaterials: непонятный файл прошёл через фильтр");
			}
			
			// закрываем, чтобы потом никто не поменял случайно
			materials = Collections.unmodifiableMap(materials);
		}
		public Map<String, Material> getMaterials() {
			if(!areMaterialsInitialized())
				throw new IllegalStateException("Dance.findMaterials: материалы не загружены для танца "+getName());
			return materials;
		}
		
		private class MaterialQuery {
			Set<String> tags;
			boolean needAudio;
			public MaterialQuery(Set<String> tags, boolean needAudio) {
				this.tags = tags;
				this.needAudio = needAudio;
			}
			public boolean match(Material m) {
				return m.tags.containsAll(tags) &&
						(!needAudio || m.hasAudio());
			}
			@Override public boolean equals(Object o) {
				if(!(o instanceof MaterialQuery)) return false;
				return tags.equals(((MaterialQuery)o).tags) &&
						needAudio == ((MaterialQuery)o).needAudio;
			}
		}
		private Map<MaterialQuery, List<Material>> materialSearchCache =
				new HashMap<MaterialQuery, List<Material>>();
		/**
		 * Возвращает отсортированный список материалов танца,
		 * соответствующих указанным критериям.
		 * @param tags Множество тегов, которые должны присутствовать у отобранных материалов. Если null, то теги не проверяются (т.е. считается пустым).
		 * @param needAudio Если true, то будут отобраны только материалы, содержащие аудиозаписи.
		 * TODO: другие типы данных
		 * TODO: поиск по отсутствию тегов
		 */
		public List<Material> findMaterials(Set<String> tags, boolean needAudio) {
			if(!areMaterialsInitialized())
				throw new IllegalStateException("Dance.findMaterials: материалы не загружены для танца "+getName());
			if(tags == null) tags = Collections.emptySet();
			MaterialQuery q = new MaterialQuery(tags, needAudio);
			if(materialSearchCache.containsKey(q))
				return materialSearchCache.get(q);
			List<Material> ret = new ArrayList<Material>();
			for(Material m: materials.values())
				if(q.match(m))
					ret.add(m);
			Collections.sort(ret);
			materialSearchCache.put(q, ret);
			return ret;
		}
		
		/**
		 * Любой танец как псевдоним ссылается как минимум сам на себя.
		 */
		@Override
		public Dance getReferringDance() {
			if(getRefCount() > 1)
				throw new IllegalStateException("На танец "+getName()+" слишком много ссылок: "+getRefCount());
			return this;
		}
	}
	/**
	 * Материал танца. Пока что поддерживаются только mp3 файлы.
	 * TODO: Может включать музыку, текст, html, видео и теги.
	 */
	public static class Material implements Comparable<Material> {
		public Dance dance;
		public String name;
		/*package*/ File audioFile;
		private Set<String> tags;
		
		public Material(Dance dance, String name) {
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
		
		/*package*/ void addTag(String tag) {
			tags.add(tag);
		}
		public Set<String> getTags() {
			return Collections.unmodifiableSet(tags);
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

	/**
	 * Запрос к списку танцев.
	 * Пока представляет собой обёртку вокруг Set<String>.
	 * @author mars
	 *
	 */
	public static class Query {
		Set<String> aliases;
		/** Конструирует запрос без псевдонимов, которому соответствует любой танец */
		public Query() {
			this.aliases = Collections.emptySet();
		}
		/** Конструирует запрос из одного псевдонима */
		public Query(String alias) {
			this.aliases = Collections.singleton(alias);
		}
		/** Конструирует запрос из нескольких псевдонимов (AND) */
		public Query(Set<String> aliases) {
			this.aliases = aliases;
		}
		/** Конструирует запрос, добавляя новый псевдоним к псевдонимам другого запроса */
		public Query(Query old, String alias) {
			aliases = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			aliases.addAll(old.aliases);
			aliases.add(alias);
		}
		/** Сопоставляет псевдонимы танца с запросом.
			Возвращает true, если танец соответствует запросу. */
		public boolean match(Dance dance) {
			return dance.aliases.containsAll(aliases);
		}
		
		/** Преобразует в массив строк, для сериализации.
		 * (массив, а не одна строка - чтобы избежать проблем со спецсимволами) */
		public String[] serialize() {
			return aliases.toArray(new String[0]);
		}
		public static Query deserialize(String[] src) {
			if(src == null) {
				Log.e("DataManager.Query", "src==null");
				throw new NullPointerException("Ошибка: src не задан");
			}
			Set<String> s = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			s.addAll(Arrays.asList(src));
			return new Query(s);
		}
		
		/** Сравнивает этот запрос с другим */
		@Override public boolean equals(Object o) {
			if(!(o instanceof Query))
				return false;
			return ((Query)o).aliases.equals(aliases);
		}
	}
}
