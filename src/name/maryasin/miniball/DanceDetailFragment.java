package name.maryasin.miniball;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
			mDance = DataManager.danceMap.get(getArguments().getString(
					ARG_DANCE_NAME));
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
					.setText("Псевдонимы танца "+mDance.getName()+":\n"+mDance.getAliases());
		}

		return rootView;
	}
}
