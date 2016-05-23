package com.example.vyomkeshjha.dps_h.Render;

        import android.app.Activity;
        import android.app.Fragment;
        import android.content.Context;
        import android.graphics.Bitmap;
        import android.graphics.pdf.PdfRenderer;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.os.Environment;
        import android.os.ParcelFileDescriptor;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
        import android.widget.SeekBar;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.example.vyomkeshjha.dps_h.AddOns.OnSwipeTouchListener;
        import com.example.vyomkeshjha.dps_h.AddOns.ZoomableImageView;
        import com.example.vyomkeshjha.dps_h.R;

        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.util.ArrayList;

/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2 {@link android.widget.Button}s to move between
 * pages. We use a {@link android.graphics.pdf.PdfRenderer} to render PDF pages as {@link android.graphics.Bitmap}s.
 */
public class PdfRendererBasicFragment extends Fragment implements View.OnClickListener {

    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    public  int pageIndex=0;

    SeekBar seek;


    public static String fileName=null;

    public coWorkerSwitcher pdfRendererWorker;


    int max=0;
    int min=0;
    int step=1;

    private TextView Footer;

    private PdfRenderer mPdfRenderer;


    private Context appContext=null;
    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;


    private ZoomableImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;

    public PdfRendererBasicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false);
    }
    int progressTrack=0;

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        seek=(SeekBar)view.findViewById(R.id.seekBar);
        max=getPageCount();
        seek.setMax((max - min) / step);
        seek.setOnSeekBarChangeListener(

                new SeekBar.OnSeekBarChangeListener()

                {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Ex :
                        // And finally when you want to retrieve the value in the range you
                        // wanted in the first place -> [3-5]
                        //
                        // if progress = 13 -> value = 3 + (13 * 0.1) = 4.3
                        int value = min + (progressTrack);
                        pageIndex=value;
                        new coWorkerSwitcher().execute(pageIndex);
                        Log.i("value","value is: "+value);

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {


                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {

                        progressTrack=progress;
                    }
                }
        );


        mImageView = (ZoomableImageView) view.findViewById(R.id.image);
        try {
            mImageView.setOnTouchListener(new OnSwipeTouchListener(appContext){
                public void onSwipeTop() {
                    seek.setVisibility(view.VISIBLE);
                }
                public void onSwipeRight() {

                    if(pageIndex>0)
                        pageIndex--;
                    new coWorkerSwitcher().execute(pageIndex);
                    Toast.makeText(appContext, pageIndex, Toast.LENGTH_SHORT).show();

                }
                public void onSwipeLeft() {

                    if(pageIndex<getPageCount()-1)
                        pageIndex++;
                    new coWorkerSwitcher().execute(pageIndex);
                    Toast.makeText(appContext, pageIndex, Toast.LENGTH_SHORT).show();
                }
                public void onSwipeBottom() {
                    seek.setVisibility(view.GONE);


                }
            });


        }
        catch (NullPointerException e)
        {

            Log.e("renderer"," "+e.toString()+ " and context is "+" image is"+mImageView);
        }

        //Footer =(TextView) view.findViewById(R.id.textView) ;
       // Footer.setText("Smile when you look at it");

        Log.i("renderer","ImageViewCreation = "+mImageView);
        getPage(pageIndex);

    }



    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);


        try {
            openRenderer(activity);


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("apples",e.getMessage());

        }

    }

    @Override
    public void onDetach() {

        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        Log.i("Resource","fileName is "+PdfRendererBasicFragment.fileName);
        mFileDescriptor = context.getAssets().openFd(PdfRendererBasicFragment.fileName).getParcelFileDescriptor();
        // This is the PdfRenderer we use to render the PDF.
        mPdfRenderer = new PdfRenderer(mFileDescriptor);

        Log.i("renderer","mImageView is "+mImageView);
        appContext=context;
                Log.i("renderer","pages = "+mPdfRenderer.getPageCount());
        Log.i("renderer","Renderer open");


    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    int iterationCounter=1;
    private Bitmap getPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return null;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);

        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(277/72*mCurrentPage.getWidth(),277/72* mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        if(mPdfRenderer.shouldScaleForPrinting())
        {

        }
        mCurrentPage.render(bitmap,null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        Log.i("currentPage",mCurrentPage.toString());
        // We are ready to show the Bitmap to user.

        Log.i("renderer","BitmapData "+bitmap);
        Log.i("renderer","ImageView "+mImageView);
        //PDFsearchHandler handler1 = new PDFsearchHandler(appContext);
        if(pageIndex==0&&iterationCounter==1) {
            mImageView.setImageBitmap(bitmap);
            iterationCounter--;
        }
return bitmap;

        // call somthing to update the UI here
    }


    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

        }
    }



    private class coWorkerSwitcher extends AsyncTask<Integer,Void,Bitmap>

    {

        @Override
        protected Bitmap doInBackground(Integer... params) {

            return getPage(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
        }
    }
    }

