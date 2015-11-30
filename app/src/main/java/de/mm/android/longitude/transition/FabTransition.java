package de.mm.android.longitude.transition;

import android.annotation.TargetApi;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.TransitionSet;

/**
 * Created by Max on 19.11.2015.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FabTransition extends TransitionSet {

//    ChangeBounds animates the bounds (location and size) of the view.
//    ChangeTransform animates the scale of the view, including the parent.
//    ChangeImageTransform allows us to change the size (and/or scale type) of the image
//    https://developer.android.com/reference/android/transition/Transition.html

    public FabTransition() {
        setOrdering(ORDERING_TOGETHER);
        addTransition(new ChangeBounds())
                .addTransition(new ChangeTransform())
                .addTransition(new ChangeImageTransform());

    }
}
