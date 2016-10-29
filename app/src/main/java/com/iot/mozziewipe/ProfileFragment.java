package com.iot.mozziewipe;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        TextView points = (TextView) view.findViewById(R.id.points);
        TextView displayName = (TextView) view.findViewById(R.id.displayName);
        TextView energy = (TextView) view.findViewById(R.id.energy);

        person = (Person) getArguments().getSerializable("person");
        displayName.setText(person.getDisplayName());
        points.setText(String.valueOf(person.getPoints()));
        energy.setText(String.valueOf(person.getEnergy()));

        return view;
    }

}
