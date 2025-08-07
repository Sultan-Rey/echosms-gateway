package com.majorware.echosms.data.models;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.majorware.echosms.R;
import com.majorware.echosms.databinding.SmsItemBinding;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SMSLogAdapter extends RecyclerView.Adapter<SMSLogAdapter.SMSLogViewHolder> {
    private final List<SMS> smsLogs;

    public SMSLogAdapter(List<SMS> smsLogs) {
        this.smsLogs =  new ArrayList<>(smsLogs);
    }

    @NonNull
    @Override
    public SMSLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SmsItemBinding binding = SmsItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SMSLogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SMSLogViewHolder holder, int position) {
        SMS sms = smsLogs.get(position);
        holder.bind(sms);
    }

    @Override
    public int getItemCount() {
        return smsLogs.size();
    }

public void updateData(List<SMS> newSmsList) {
    this.smsLogs.clear();
    this.smsLogs.addAll(newSmsList);
    notifyDataSetChanged();
}
    public static class SMSLogViewHolder extends RecyclerView.ViewHolder {
        private final SmsItemBinding binding;

        public SMSLogViewHolder(SmsItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SMS sms) {
            // 🔥 Conversion de Timestamp en Date
            Timestamp timestamp = sms.getCreatedAt();
            Date date = timestamp.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedTime = sdf.format(date);
            String _stringSMS_details = binding.getRoot().getContext().getResources().getString(R.string.sent_sms)+" "+ sms.getPhoneNumber();
            binding.timestampTextview.setText(formattedTime);
            binding.messageTextview.setText(_stringSMS_details);
            binding.statusTextview.setText(sms.getStatus());

            int color = sms.getStatus().equalsIgnoreCase("Sent") ?
                    binding.getRoot().getContext().getResources().getColor(android.R.color.holo_green_dark) :
                    binding.getRoot().getContext().getResources().getColor(android.R.color.holo_red_dark);

            binding.statusTextview.setTextColor(color);
        }
    }
}

