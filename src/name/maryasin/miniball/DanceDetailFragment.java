package name.maryasin.miniball;

import java.io.IOException;
import java.util.List;

import com.actionbarsherlock.app.SherlockFragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.DataManager.Material;

/**
 * A fragment representing a single Dance detail screen. This fragment is either
 * contained in a {@link DanceListActivity} in two-pane mode (on tablets) or a
 * {@link DanceDetailActivity} on handsets.
 */
public class DanceDetailFragment extends SherlockFragment implements OnItemClickListener {
	/**
	 * The fragment argument representing the name of dance that this fragment
	 * represents.
	 */
	public static final String ARG_DANCE_NAME = "dance_name";

	/**
	 * The dance this fragment is presenting.
	 */
	private DataManager.Dance mDance;
	private List<DataManager.Material> mMaterialList;

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
			if(!mDance.areMaterialsInitialized())
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
			mMaterialList = mDance.findMaterials(null, true);
			getActivity().setTitle(mDance.getName()); // FIXME: а что будет в планшетном режиме?
			((ListView) rootView.findViewById(R.id.material_audio_list))
					.setAdapter(new ArrayAdapter<DataManager.Material>(
							getActivity(),
							android.R.layout.simple_list_item_1,
							android.R.id.text1,
							mMaterialList) {
						@Override
						public View getView(int position,
								View convertView, ViewGroup parent) {
							Material m = mMaterialList.get(position);
							View row = convertView;
							if(row == null) {
								LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
								row = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
							}
							((TextView)row.findViewById(android.R.id.text1))
									.setText(m.name);
							if(m.getTags().size() > 0)
								((TextView)row.findViewById(android.R.id.text2))
										.setText(m.getTags().toString());
							return row;
						}
					});
		}
		((ListView) rootView.findViewById(R.id.material_audio_list))
			.setOnItemClickListener(this);

		return rootView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		Material m = mMaterialList.get(pos);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.fromFile(m.getAudioFile()), "audio/*");
		startActivity(i);
	}
}
