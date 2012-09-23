package com.nokia.example.picasa.s40;

import com.futurice.tantalum3.PlatformUtils;
import com.futurice.tantalum3.TantalumMIDlet;
import com.futurice.tantalum3.Worker;
import com.futurice.tantalum3.log.L;
import com.futurice.tantalum3.net.StaticWebCache;
import com.nokia.example.picasa.common.PicasaStorage;
import javax.microedition.lcdui.*;

public final class PicasaViewer extends TantalumMIDlet implements CommandListener {

    FeaturedCanvas featuredView;
    SearchCanvas searchView;
    private DetailedCanvas detailedView;
    private Displayable lastView;
    private CategoryBarHandler categoryBarHandler = null;
    private Command featuredCommand = new Command("Home", Command.SCREEN, 0);
    private Command searchCommand = new Command("Search", Command.SCREEN, 1);
    private Command backCommand = new Command("Back", Command.BACK, 0);
    private Command exitCommand = new Command("Exit", Command.EXIT, 0);
    private Command exitCommand2 = new Command("Exit", Command.EXIT, 0);
    private Command refreshCommand = new Command("Refresh", Command.OK, 0);

    public PicasaViewer() {
        super(4);
    }

    public void startApp() {
        featuredView = new FeaturedCanvas(this);
        PicasaStorage.init(featuredView.getWidth()); // Initialize storage with display width.
        try {
            featuredView.loadFeed(null, StaticWebCache.GET_ANYWHERE).join(2000);
        } catch (Exception ex) {
            //#debug
            L.e("Slow initial feed load", null, ex);
        }
        lastView = featuredView;
        try {
            final Class cbc = Class.forName("com.nokia.example.picasa.s40.CategoryBarHandler");
            CategoryBarHandler.setMidlet(this);
            categoryBarHandler = (CategoryBarHandler) cbc.newInstance();
            refreshCommand = (Command) Class.forName("com.nokia.example.picasa.s40.UpdateIconCommand").newInstance();
        } catch (Throwable t) {
            //#debug
            L.i("Can not set category bar handler", "normal before SDK 2.0");
        }
        featuredView.addCommand(refreshCommand);
        featuredView.setCommandListener(PicasaViewer.this);
        Display.getDisplay(this).setCurrent(featuredView);

        // Continue init tasks
        detailedView = new DetailedCanvas(PicasaViewer.this);
        detailedView.setCommandListener(PicasaViewer.this);
        searchView = new SearchCanvas(PicasaViewer.this);
        searchView.setCommandListener(PicasaViewer.this);
        if (categoryBarHandler == null) {
            try {
                /*
                 * Fallback when there is no category bar (Nokia SDK 1.1 and earlier)
                 */
                featuredView.addCommand(searchCommand);
                featuredView.addCommand(exitCommand);
                detailedView.addCommand(backCommand);
                detailedView.setCommandListener(PicasaViewer.this);
                searchView.addCommand(featuredCommand);
                if (phoneSupportsCategoryBar()) {
                    searchView.addCommand(exitCommand2);
                }
                searchView.setCommandListener(PicasaViewer.this);
            } catch (Exception ex) {
                //#debug
                L.e("Can not fallback to command init on other views", "", ex);
            }
        }
    }

    public void setDetailed() {
//        PlatformUtils.runOnUiThread(new Runnable() {
//            public void run() {
//                if (phoneSupportsCategoryBar()) {
//                    categoryBarHandler.setVisibility(false);
//                }
                lastView = Display.getDisplay(PicasaViewer.this).getCurrent();

                Display.getDisplay(PicasaViewer.this).setCurrent(detailedView);
//            }
//        });
     //   setCategoryBarVisibility(false);
    }

    public void goBack() {
        if (phoneSupportsCategoryBar()) {
            categoryBarHandler.goBack();
        }
        Display.getDisplay(this).setCurrent(lastView);
    }

    public void stopReloadAnimation() {
        try {
            ((UpdateIconCommand) refreshCommand).stopAnimation();
        } catch (Throwable t) {
            //#debug
            L.e("Can not stop reload animation", null, t);
        }
    }

    public void startReloadAnimation() {
        try {
            ((UpdateIconCommand) refreshCommand).startAnimation(GestureCanvas.spinTimer);
        } catch (Throwable t) {
            //#debug
            L.e("Can not start reload animation", null, t);
        }
    }

    public void commandAction(Command c, Displayable d) {
        //#debug
        L.i("Command action", "Command " + c);
        if (c.getCommandType() == Command.BACK) {
            goBack();
        } else if (c == refreshCommand) {
            featuredView.refresh(null, StaticWebCache.GET_WEB);
        } else if (c == featuredCommand) {
            Display.getDisplay(this).setCurrent(featuredView);
        } else if (c == searchCommand) {
            Display.getDisplay(this).setCurrent(searchView);
        } else if (c.getCommandType() == Command.EXIT) {
            Worker.shutdown(false);
        } else if (c.getCommandType() == Command.CANCEL) {
            searchView.deleteChar();
        }
    }

    public boolean phoneSupportsCategoryBar() {
        return categoryBarHandler != null;
    }
    
    public void setCategoryBarVisibility(final boolean visibility) {
        if (categoryBarHandler != null) {
            categoryBarHandler.setVisibility(visibility);
        }
    }
}
