package name.maryasin.miniball;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.DataManager.Dance;

/**
 * A fragment representing a single Tag detail screen.
 * This fragment is either contained in a {@link TagListActivity}
 * in two-pane mode (on tablets) or a {@link DanceListActivity}
 * on handsets.
 */
public class DanceListFragment extends ListFragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private List<DataManager.Dance> mDances;
    
    public interface Callbacks {
        public void onItemSelected(String id);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DanceListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
        	String tag = getArguments().getString(ARG_ITEM_ID);
        	mDances = DataManager.findDances(new DataManager.Query(tag));

            setListAdapter(new ArrayAdapter<Dance>(
                    getActivity(),
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    mDances));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dance_list, container, false);

        // Show the dummy content as text in a TextView.
        if (mDances != null) {
            //FIXME ((TextView) rootView.findViewById(R.id.tag_detail)).setText(mDance.name);
        }

        return rootView;
    }
}
