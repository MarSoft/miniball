package name.maryasin.miniball;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.DataManager.Alias;
import name.maryasin.miniball.data.DataManager.Dance;
import name.maryasin.miniball.data.DataManager.DataChangedListener;
import name.maryasin.miniball.data.DataManager.Query;

/**
 * A list fragment representing a list of Dances. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link DanceDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class DanceListFragment extends ListFragment
		implements android.widget.AdapterView.OnItemLongClickListener,
		DataManager.DataChangedListener {
	/** Имя аргумента фрагмента, хранящего перечень задействованных тегов */
	public static final String ARG_TAGS_FILTER = "tags_filter";
	public static final String ARG_TWOPANE = "two_pane";
	
	/** Текущий запрос к списку танцев (TODO: в заголовок его) */
	private DataManager.Query query;
	/** Список танцев (и псевдонимов!), отображаемый в activity */
	private List<DataManager.Alias> danceList;

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onDanceSelected(String name);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onDanceSelected(String id) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DanceListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DataManager.addDataChangedListener(this);
		onDataChanged(); // load data
	}
	@Override
	public void onDestroy() {
		DataManager.removeDataChangedListener(this);
		super.onDestroy();
	}
	@Override
	public void onDataChanged() {
		String[] query_s = null;
		if(getArguments() != null && getArguments().containsKey(ARG_TAGS_FILTER))
			query_s = getArguments().getStringArray(ARG_TAGS_FILTER);
		if(query_s != null) { // если указан И не null
			// Загружаем переданный перечень тегов
			query = Query.deserialize(
					getArguments().getStringArray(ARG_TAGS_FILTER));
		} else { // если ничего не указано
			query = new DataManager.Query(); // создаём пустой запрос
		}
		if(!DataManager.isDanceListInitialized())
			try {
				DataManager.initDanceList();
			} catch (IOException e) {
				Log.e("DanceListFragment", "Ошибка инициализации DataManager", e);
				Toast.makeText(getActivity(), "Не удалось инициализировать данные!\nОшибка: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				setListAdapter(new ArrayAdapter<String>(getActivity(), -1, -1, new String[0]));
				return;
			}
		danceList = DataManager.findAliases(query);
		setListAdapter(new ArrayAdapter<DataManager.Alias>(getActivity(),
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1, danceList));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		getListView().setFastScrollEnabled(true);
		getListView().setOnItemLongClickListener(this);
		
		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
		if(getArguments() != null && getArguments().containsKey(ARG_TWOPANE))
			this.setActivateOnItemClick(getArguments().getBoolean(ARG_TWOPANE));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		Alias aod = danceList.get(position);
		if(aod.getRefCount() > 1) { // если ссылается не только на себя (FIXME: танец всё же по умолчанию показываем?)
			Query q = new Query(query, aod.getName()); // создаём новый запрос, уточнённый
			Bundle args = new Bundle();
			args.putStringArray(ARG_TAGS_FILTER, q.serialize());
			if(this.getActivateOnItemClick())
				args.putBoolean(ARG_TWOPANE, true);
			DanceListFragment n = new DanceListFragment();
			n.setArguments(args);
			getActivity().getSupportFragmentManager().beginTransaction()
					.replace(R.id.dance_list_container, n)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
					.addToBackStack(null)
					.commit();
		} else {
			// Сообщаем activity, в которой мы установлены, чтобы открыла
			// соответствующие danceDetails.
			// getReferringDance: безопасно, т.к. refCount заведомо <= 1 (см. if).
			mCallbacks.onDanceSelected(aod.getReferringDance().getName());
		}
	}
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Alias aod = danceList.get(position);
		if(aod instanceof Dance) { // танец -> открываем, не смотря на рефы
			mCallbacks.onDanceSelected(aod.getName());
			return true;
		} else { // не обрабатываем
			return false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	private boolean twopane;
	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
		twopane = activateOnItemClick;
	}
	public boolean getActivateOnItemClick() {
		return twopane;
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
}
