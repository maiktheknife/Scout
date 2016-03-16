package de.mm.android.longitude.recyclerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.pkmmte.view.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.mm.android.longitude.R;
import de.mm.android.longitude.model.ContactData;
import de.mm.android.longitude.util.StorageUtil;

/**
 * Created by Max on 21.03.2015.
 */
public class ContactRecyclerAdapter extends RecyclerView.Adapter<ContactRecyclerAdapter.MyRecyclerViewHolder> {
    private static final String TAG = ContactRecyclerAdapter.class.getName();
    private Context context;
    private MyRecyclerViewHolder.CardActionListener listener;
    private ArrayList<ContactData> data;
    private Location location;

    public ContactRecyclerAdapter(Context context, Location myLocation, MyRecyclerViewHolder.CardActionListener listener) {
        this.context = context;
        this.location = myLocation;
        this.listener = listener;
        this.data = new ArrayList<>();
    }

    @Override
    public MyRecyclerViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.listitem_contact, viewGroup, false);
        return new MyRecyclerViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(MyRecyclerViewHolder holder, int position) {
        ContactData c = data.get(position);
        PopupMenu menu = new PopupMenu(context, holder.cardMenu);
        if (c.is_confirmed()) {
            holder.itemView.setEnabled(true);
            holder.latestUpdate.setText(c.getUpdatedOnFormatted());
            holder.address.setText(context.getResources().getString(R.string.address_distance, c.getAddress(), Math.round(c.getDistanceTo(location))));
            holder.address.setSelected(true);
            Bitmap pic = StorageUtil.loadBitmap(context, c.getEmail());
            if (pic != null) {
                holder.image.setImageBitmap(pic);
            }

            menu.inflate(R.menu.f_contacts_overflow);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_f_contact_overflow_poke:
                        listener.onCardPoked(position);
                        break;
                    case R.id.menu_f_contact_overflow_delete:
                        listener.onCardDeleted(position);
                        break;
                    default:
                        break;
                }
                return true;
            });
        } else {
            holder.itemView.setEnabled(false);
            menu.inflate(R.menu.f_contacts_overflow_unconfirmed);
            menu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_f_contact_overflow_confirm:
                        listener.onCardConfirmed(position, true);
                        break;
                    case R.id.menu_f_contact_overflow_refuse:
                        listener.onCardConfirmed(position, false);
                        break;
                    default:
                        break;
                }
                return true;
            });
        }
        holder.cardMenu.setOnClickListener(v -> menu.show());
        holder.name.setText(c.getName());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    /* Stuff */

    public void addItem(ContactData c) {
        Log.d(TAG, "addItem: " + c);
        data.add(0, c);
        notifyItemInserted(0);
    }

    public void addItems(List<ContactData> c) {
        Log.d(TAG, "addItems");
        data.addAll(c);
        notifyItemRangeInserted(0, c.size());
    }

    public void removeItem(int position) {
        Log.d(TAG, "removeItem: " + position + " of " + data.size());
        data.remove(position);
        notifyItemRemoved(position);
    }

    public void removeItems(List<Integer> positions) {
        Log.d(TAG, "removeItems");
        for (Integer i : positions) {
            removeItem(i);
        }
    }

    public ContactData getItem(int position) {
        Log.d(TAG, "getItem: " + position + " of " + data.size());
        return data.get(position);
    }

    public ArrayList<ContactData> getItems() {
        return data;
    }

    public void clear(){
       int size = data.size();
       data.clear();
       notifyItemRangeRemoved(0, size);
    }

    public Location getLocation() {
        return location;
    }

    /* ViewHolder */

    public static class MyRecyclerViewHolder extends RecyclerView.ViewHolder {

        public interface CardActionListener {
            void onCardClicked(int position);
            void onCardPoked(int position);
            void onCardConfirmed(int position, boolean isAccepted);
            void onCardDeleted(int position);
        }

        @Bind(R.id.contactrow_image)
        CircularImageView image;
        @Bind(R.id.contactrow_name)
        TextView name;
        @Bind(R.id.contactrow_latestupdate)
        TextView latestUpdate;
        @Bind(R.id.contactrow_address)
        TextView address;
        @Bind(R.id.contactrow_menu)
        ImageView cardMenu;

        public MyRecyclerViewHolder(View itemView, CardActionListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(v -> listener.onCardClicked(getAdapterPosition()));
        }
    }
}