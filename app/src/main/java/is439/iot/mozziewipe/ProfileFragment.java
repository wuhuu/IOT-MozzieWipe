package is439.iot.mozziewipe;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class ProfileFragment extends Fragment {

    private Person person;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(Person person) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("person", person);
        fragment.setArguments(bundle);
        return fragment;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //Load the UI for profile fragment
        ImageView profilePicImage = (ImageView) view.findViewById(R.id.imageProfilePic);
        ImageView trophyImage = (ImageView) view.findViewById(R.id.imageTrophy);
        ImageView flameImage = (ImageView) view.findViewById(R.id.imageFlame);
        TextView pointsText = (TextView) view.findViewById(R.id.textViewPoint);
        TextView displayNameText = (TextView) view.findViewById(R.id.textViewDisplayName);
        TextView energyText = (TextView) view.findViewById(R.id.textViewEnergy);
        TextView levelText = (TextView) view.findViewById(R.id.textViewLevel);

        //initialize the value
        person = (Person) getArguments().getSerializable("person");

        trophyImage.setImageResource(R.drawable.trophy);
        flameImage.setImageResource(R.drawable.flame);
        displayNameText.setText(person.getDisplayName());
        energyText.setText(String.valueOf(person.getEnergy()));
        pointsText.setText(String.valueOf(person.getPoints()));
        int points = person.getPoints() / 10;
        switch (points) {
            case 0:
                profilePicImage.setImageResource(R.drawable.lvl1);
                levelText.setText(R.string.level1);
                break;
            case 1:
                profilePicImage.setImageResource(R.drawable.lvl1);
                levelText.setText(R.string.level1);
                break;
            case 2:
                profilePicImage.setImageResource(R.drawable.lvl2);
                levelText.setText(R.string.level2);
                break;
            case 3:
                profilePicImage.setImageResource(R.drawable.lvl3);
                levelText.setText(R.string.level3);
                break;
            case 4:
                profilePicImage.setImageResource(R.drawable.lvl4);
                levelText.setText(R.string.level4);
                break;
            case 5:
                profilePicImage.setImageResource(R.drawable.lvl5);
                levelText.setText(R.string.level5);
                break;
            case 6:
                profilePicImage.setImageResource(R.drawable.lvl6);
                levelText.setText(R.string.level6);
                break;
            case 7:
                profilePicImage.setImageResource(R.drawable.lvl7);
                levelText.setText(R.string.level7);
                break;
            default:
                profilePicImage.setImageResource(R.drawable.lvl8);
                levelText.setText(R.string.level8);
        }

        return view;
    }

}
