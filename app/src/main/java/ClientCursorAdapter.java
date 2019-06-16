import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.example.aheli.newsreader.R;

public class TodoCursorAdapter extends CursorAdapter {
    public TodoCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.cursor_row, parent, false);
    }
    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.


    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView newstitle =  view.findViewById(R.id.newsTitle);
        TextView newscontent = view.findViewById(R.id.newsContent);
        // Extract properties from cursor

        String t = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String c = cursor.getString(cursor.getColumnIndexOrThrow("content"));
        // Populate fields with extracted properties
        newstitle.setText(t);
        newscontent.setText(c);
    }
}
