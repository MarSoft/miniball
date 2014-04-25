package name.maryasin.miniball.data;

import java.io.*;
import java.util.*;

import android.util.Log;

/**
 * Этот класс представляет один танец из базы данных
 */
public class Dance extends Alias {
	private static final String TAG = "Dance";

	private File danceRoot;
	private Set<String> aliases;
	private Map<String, Material> materials;

	/** После инициализации обязательно вызвать initAliases() */
	/*package*/ Dance(String name) {
		super(name);
		refCount = 1; // любой танец ссылается сам на себя, как минимум
		danceRoot = new File(DataManager.rootPath, name);
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
			return m.getTags().containsAll(tags) &&
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
