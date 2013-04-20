package name.maryasin.miniball;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import name.maryasin.miniball.R;

/**
 * An activity representing a list of Dances. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link DanceDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link DanceListFragment} and the item details (if present) is a
 * {@link DanceDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link DanceListFragment.Callbacks} interface to listen for item selections.
 */
public class DanceListActivity extends SherlockFragmentActivity implements
		DanceListFragment.Callbacks {
	public static final String TAG = "DanceListActivity";

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dance_list);
		
		DanceListFragment danceListFr = new DanceListFragment();
		Bundle args = new Bundle();
		if(savedInstanceState == null && getIntent().hasExtra(DanceListFragment.ARG_TAGS_FILTER)) {
			args.putStringArray(DanceListFragment.ARG_TAGS_FILTER,
					getIntent().getStringArrayExtra(DanceListFragment.ARG_TAGS_FILTER));
		}
		if (findViewById(R.id.dance_detail_container) != null) {
			// The detail container view will be present only in the
			// large-screen layouts (res/values-large and
			// res/values-sw600dp). If this view is present, then the
			// activity should be in two-pane mode.
			mTwoPane = true;

			// In two-pane mode, list items should be given the
			// 'activated' state when touched.
			// Так что передаём соответствующий аргумент фрагменту.
			args.putBoolean(DanceListFragment.ARG_TWOPANE, true);
		}
		danceListFr.setArguments(args);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.dance_list_container, danceListFr)
				.commit();

		// TODO: If exposing deep links into your app, handle intents here.
	}

	/**
	 * Callback method from {@link DanceListFragment.Callbacks} indicating that
	 * the dance with the given ID was selected.
	 */
	@Override
	public void onDanceSelected(String name) {
		if (mTwoPane) {
			// In two-pane mode, show the detail view in this activity by
			// adding or replacing the detail fragment using a
			// fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(DanceDetailFragment.ARG_DANCE_NAME, name);
			DanceDetailFragment fragment = new DanceDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.dance_detail_container, fragment).commit();

		} else {
			// In single-pane mode, simply start the detail activity
			// for the selected item ID.
			Intent detailIntent = new Intent(this, DanceDetailActivity.class);
			detailIntent.putExtra(DanceDetailFragment.ARG_DANCE_NAME, name);
			startActivity(detailIntent);
		}
	}
}
