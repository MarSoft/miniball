package name.maryasin.miniball;

import java.io.IOException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;

/**
 * A fragment representing a single Dance detail screen. This fragment is either
 * contained in a {@link DanceListActivity} in two-pane mode (on tablets) or a
 * {@link DanceDetailActivity} on handsets.
 */
public class DanceDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the name of dance that this fragment
	 * represents.
	 */
	public static final String ARG_DANCE_NAME = "dance_name";

	/**
	 * The dance this fragment is presenting.
	 */
	private DataManager.Dance mDance;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public DanceDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_DANCE_NAME)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			String danceName = getArguments().getString(
					ARG_DANCE_NAME);
			mDance = DataManager.danceMap.get(danceName); // FIXME: если не инициализировано, создавать новый? (чтобы не требовалась загрузка, и чтобы не падать при неинициализированном менеджере)
			if(mDance == null) { // нет такого танца
				Log.e("DanceDetailFragment", "Танец не найден: "+danceName);
				return;
			}
			if(!mDance.areMaterialsLoaded())
				try {
					mDance.initMaterials();
				} catch (IOException e) {
					Log.e("DanceDetailFragment",
							"Не удалось загрузить материалы танца "+danceName, e);
				}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_dance_detail,
				container, false);

		// Show the dance content as text in a TextView.
		if (mDance != null) {
			((TextView) rootView.findViewById(R.id.dance_detail))
					.setText("Псевдонимы танца "+mDance.getName()+":\n"
							+mDance.getAliases()
							+"\nRefCount: "+mDance.getRefCount()
							+"\nМатериалы: "+mDance.getMaterials().values());
		}

		return rootView;
	}
}
