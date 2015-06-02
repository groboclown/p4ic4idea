package net.groboclown.idea.p4ic.ui.revision;

import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Shows a list of file revisions.  This has problems with rendering stuff
 * inside the dialog, so it's not used in the plugin.xml file.
 */
public class RevisionDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    //private RevisionTable revisionList;
    private RevisionList revisionList;
    //private RevisionTreeTable revisionList;
    private AsyncResult<P4FileRevision> result = new AsyncResult<P4FileRevision>();

    public RevisionDialog(@Nullable Frame owner, @NotNull P4Vcs vcs, @NotNull VirtualFile file) {
        super(owner, file.getCanonicalPath());
        contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        final JPanel buttonPanel = new JPanel();
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(Box.createHorizontalGlue());

        buttonCancel = new JButton();
        buttonPanel.add(buttonCancel);
        buttonCancel.setText(P4Bundle.getString("ui.Cancel"));
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        buttonOK = new JButton();
        buttonPanel.add(buttonOK);
        buttonOK.setText(P4Bundle.getString("ui.OK"));
        buttonOK.setEnabled(false);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        //revisionList = new RevisionTable(vcs, file);
        revisionList = new RevisionList(vcs, file);
        //revisionList = new RevisionTreeTable(vcs, file);
        contentPane.add(revisionList, BorderLayout.CENTER);
        revisionList.addRevisionSelectedListener(new RevisionSelectedListener() {
            @Override
            public void revisionSelected(@Nullable final P4FileRevision rev) {
                buttonOK.setEnabled(rev != null);
            }
        });

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        if (! revisionList.isValid()) {
            // initial validation problem
            getRootPane().setDefaultButton(buttonCancel);
        }

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }


    public AsyncResult<P4FileRevision> showAndGet() {
        pack();
        //final Dimension bounds = getSize();
        //final Rectangle parentBounds = getOwner().getBounds();
        //setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
        //        parentBounds.y + (parentBounds.height - bounds.height) / 2);
        setVisible(true);
        return result;
    }


    @Nullable
    public static P4FileRevision requestRevision(@NotNull P4Vcs vcs, @NotNull VirtualFile file) {
        if (vcs.getProject().isDisposed()) {
            return null;
        }
        ensureEventDispatchThread();

        // TODO get real parent window
        final RevisionDialog dialog = new RevisionDialog(null, vcs, file);
        return dialog.showAndGet().getResult();
    }


    private void onOK() {
        result.setDone(revisionList.getSelectedRevision());

        dispose();
    }

    private void onCancel() {
        result.setDone(null);

        dispose();
    }

    /**
     * Ensure that dialog is used from even dispatch thread.
     *
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    private static void ensureEventDispatchThread() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException(
                    "The DialogWrapper can only be used in event dispatch thread. Current thread: " + Thread
                            .currentThread());
        }
    }
}
