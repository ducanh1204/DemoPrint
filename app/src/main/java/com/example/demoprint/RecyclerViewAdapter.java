package com.example.demoprint;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    List<BluetoothDevice> bluetoothDeviceList;
    public interface IOnClickItem {
        void onClick(int position);
    }
    private IOnClickItem iOnClickItem;

    public RecyclerViewAdapter(List<BluetoothDevice> bluetoothDeviceList, IOnClickItem iOnClickItem) {
        this.bluetoothDeviceList = bluetoothDeviceList;
        this.iOnClickItem = iOnClickItem;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_list_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        if(bluetoothDeviceList.get(position).getName()!=null){
            holder.tvDevice.setText(bluetoothDeviceList.get(position).getName());
        }else holder.tvDevice.setText(bluetoothDeviceList.get(position).getAddress());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(iOnClickItem!=null){
                    iOnClickItem.onClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bluetoothDeviceList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDevice;
        public ViewHolder(@NonNull  View itemView) {
            super(itemView);
            tvDevice = itemView.findViewById(R.id.tvDevice);
        }
    }
}
