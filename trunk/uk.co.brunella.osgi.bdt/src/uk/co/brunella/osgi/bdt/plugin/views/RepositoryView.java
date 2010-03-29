/*
 * Copyright 2008 brunella ltd
 *
 * Licensed under the GPL Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.brunella.osgi.bdt.plugin.views;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.prefs.BackingStoreException;

import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor;
import uk.co.brunella.osgi.bdt.bundle.BundleRepository;
import uk.co.brunella.osgi.bdt.bundle.Version;
import uk.co.brunella.osgi.bdt.bundle.VersionRange;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ExportPackage;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.ImportPackage;
import uk.co.brunella.osgi.bdt.bundle.BundleDescriptor.RequireBundle;
import uk.co.brunella.osgi.bdt.plugin.Activator;
import uk.co.brunella.osgi.bdt.repository.BundleRepositoryPersister;
import uk.co.brunella.osgi.bdt.repository.Deployer;

public class RepositoryView extends ViewPart {

  private TreeViewer viewer;
  private ViewContentProvider contentProvider;
  private DrillDownAdapter drillDownAdapter;
  private Action actionRemoveRepository;
  private Action actionAddRepository;
  private Action actionRemoveBundle;
  private Action actionAddBundle;
  private Action actionRefresh;

  enum TreeObjectType {
    ROOT, REPOSITORY, BUNDLE, IMPORT_PACKAGE, EXPORT_PACKAGE, IMPORT_PACKAGE_LABEL, EXPORT_PACKAGE_LABEL,
    REQUIRED_BUNDLES_LABEL, REQUIRED_BUNDLE
  }

  class TreeObject implements IAdaptable {
    private String name;
    private TreeParent parent;
    private TreeObjectType type;
    private Object value;
    protected boolean resolved;
    protected boolean optional;

    public TreeObject(String name, TreeObjectType type, Object value, boolean resolved, boolean optional) {
      this.name = name;
      this.type = type;
      this.value = value;
      this.resolved = resolved;
      this.optional = optional;
    }

    public String getName() {
      return name;
    }

    public TreeObjectType getType() {
      return type;
    }

    public void setParent(TreeParent parent) {
      this.parent = parent;
    }

    public TreeParent getParent() {
      return parent;
    }

    public Object getValue() {
      return value;
    }
    
    public boolean isResolved() {
      return resolved;
    }

    public boolean isOptional() {
      return optional;
    }
    
    public String toString() {
      return getName();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class key) {
      return null;
    }
  }

  class TreeParent extends TreeObject {
    private List<TreeObject> children;

    public TreeParent(String name, TreeObjectType type, Object value) {
      super(name, type, value, true, false);
      children = new ArrayList<TreeObject>();
    }

    public void addChild(TreeObject child) {
      children.add(child);
      child.setParent(this);
    }

    public void removeChild(TreeObject child) {
      children.remove(child);
      child.setParent(null);
    }

    public TreeObject[] getChildren() {
      return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
    }

    public boolean hasChildren() {
      return children.size() > 0;
    }
    
    public void setResolved(boolean resolved) {
      this.resolved = resolved;
    }
    
    public void setOptional(boolean optional) {
      this.optional = optional;
    }
  }

  class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
    private TreeParent invisibleRoot;

    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

    public Object[] getElements(Object parent) {
      if (parent.equals(getViewSite())) {
        if (invisibleRoot == null)
          initialize();
        return getChildren(invisibleRoot);
      }
      return getChildren(parent);
    }

    public Object getParent(Object child) {
      if (child instanceof TreeObject) {
        return ((TreeObject) child).getParent();
      }
      return null;
    }

    public Object[] getChildren(Object parent) {
      if (parent instanceof TreeParent) {
        return ((TreeParent) parent).getChildren();
      }
      return new Object[0];
    }

    public boolean hasChildren(Object parent) {
      if (parent instanceof TreeParent)
        return ((TreeParent) parent).hasChildren();
      return false;
    }

    public void refresh() {
      invisibleRoot = null;
    }

    private void initialize() {
      invisibleRoot = new TreeParent("", TreeObjectType.ROOT, null);
      String[] repositoryPaths = getRepositoryPaths();
      for (int i = 0; i < repositoryPaths.length; i++) {
        BundleRepositoryPersister persister = new BundleRepositoryPersister(new File(repositoryPaths[i]));
        BundleRepository repository = null;
        try {
          repository = persister.load();
        } catch (IOException e) {
          repository = null;
        }
        invisibleRoot.addChild(createTreeParent(repository, repositoryPaths[i]));
      }
    }

    private TreeObject createTreeParent(BundleRepository repository, String name) {
      if (repository == null) {
        TreeObject root = new TreeObject(name + " [Cannot load repository]", TreeObjectType.REPOSITORY, name, false, false);
        return root;
      } else {
        TreeParent root = new TreeParent(name + " [" + repository.getProfileName() + "]", TreeObjectType.REPOSITORY, repository);
        for (BundleDescriptor descriptor : repository.getBundleDescriptors()) {
          String label = descriptor.getBundleSymbolicName() + " (" + descriptor.getBundleVersion() + ")";
          if (descriptor.getFragmentHost() != null) {
            label += " [fragment host: " + descriptor.getFragmentHost() + "]"; 
          }
          label += " - supplied by: " + descriptor.getBundleJarFileName();
          TreeParent bundle = new TreeParent(label, TreeObjectType.BUNDLE, descriptor);
          root.addChild(bundle);
          boolean bundleIsResolved = true;
          boolean bundleMissingMandatory = false;
          if (descriptor.getImportPackages().length > 0) {
            TreeParent importedPackages = new TreeParent("Imported Packages", TreeObjectType.IMPORT_PACKAGE_LABEL,
                descriptor.getImportPackages());
            bundle.addChild(importedPackages);
            for (ImportPackage importPackage : descriptor.getImportPackages()) {
              label = importPackage.getName() + " " + cleanupVersionRange(importPackage.getVersionRange());
              if (!importPackage.isMandatory()) {
                label += "  optional";
              }
              boolean resolved = repository.resolve(importPackage.getName(), importPackage.getVersionRange(), 
                  importPackage.isMandatory()).length > 0;
              TreeObject node = new TreeObject(label, TreeObjectType.IMPORT_PACKAGE, importPackage,
                  resolved, !importPackage.isMandatory());
              if (!resolved) {
                bundleIsResolved = false;
                if (importPackage.isMandatory()) {
                  bundleMissingMandatory = true;
                }
              }
              importedPackages.addChild(node);
            }
          }
          if (descriptor.getExportPackages().length > 0) {
            TreeParent exportedPackages = new TreeParent("Exported Packages", TreeObjectType.IMPORT_PACKAGE_LABEL,
                descriptor.getExportPackages());
            bundle.addChild(exportedPackages);
            for (ExportPackage exportPackage : descriptor.getExportPackages()) {
              label = exportPackage.getName() + " " + exportPackage.getVersion();
              TreeObject node = new TreeObject(label, TreeObjectType.EXPORT_PACKAGE, exportPackage, true, false);
              exportedPackages.addChild(node);
            }
          }
          if (descriptor.getRequireBundles().length > 0) {
            TreeParent requiredBundles = new TreeParent("Required Bundles", TreeObjectType.REQUIRED_BUNDLES_LABEL,
                descriptor.getRequireBundles());
            bundle.addChild(requiredBundles);
            for (RequireBundle requireBundle : descriptor.getRequireBundles()) {
              label = requireBundle.getName() + " " + cleanupVersionRange(requireBundle.getVersionRange());
              if (!requireBundle.isMandatory()) {
                label += "  optional";
              }
              if (requireBundle.isReexport()) {
                if (!requireBundle.isMandatory()) {
                  label += ", re-export";
                } else {
                  label += "  re-export";
                }
              }
              boolean resolved = repository.resolveBundle(requireBundle.getName(), requireBundle.getVersionRange(), 
                  requireBundle.isMandatory()).length > 0;
              TreeObject node = new TreeObject(label, TreeObjectType.REQUIRED_BUNDLE, requireBundle, resolved, !requireBundle.isMandatory());
              requiredBundles.addChild(node);
              if (!resolved) {
                bundleIsResolved = false;
                if (requireBundle.isMandatory()) {
                  bundleMissingMandatory = true;
                }
              }
            }
          }
          bundle.setResolved(bundleIsResolved);
          bundle.setOptional(!bundleMissingMandatory);
        }
        return root;
      }
    }
  }

  private String cleanupVersionRange(VersionRange versionRange) {
    String range = versionRange.toString();
    if (range.equals("[0.0.0,2147483647.2147483647.2147483647]")) {
      return "";
    } else if (range.endsWith(",2147483647.2147483647.2147483647]")) {
      return range.substring(1, range.indexOf(','));
    } else {
      return range;
    }
  }

  class ViewLabelProvider extends LabelProvider implements IColorProvider {

    private Image pluginImage;
    private Image reqPluginImage;
    private Image packageImage;
    private Image packageFolderImage;
    private Image folderImage;

    public ViewLabelProvider() {
      pluginImage = Activator.getImageDescriptor("icons/plugin_obj.gif").createImage();
      reqPluginImage = Activator.getImageDescriptor("icons/req_plugins_obj.gif").createImage();
      packageImage = Activator.getImageDescriptor("icons/package_obj.gif").createImage();
      packageFolderImage = Activator.getImageDescriptor("icons/packagefolder_obj.gif").createImage();
      folderImage = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER).createImage();
    }

    public String getText(Object obj) {
      return obj.toString();
    }

    public Image getImage(Object obj) {
      if (obj instanceof TreeObject) {
        TreeObjectType type = ((TreeObject) obj).getType();
        if (type == TreeObjectType.IMPORT_PACKAGE || type == TreeObjectType.EXPORT_PACKAGE) {
          return packageImage;
        } else if (type == TreeObjectType.IMPORT_PACKAGE_LABEL || type == TreeObjectType.EXPORT_PACKAGE_LABEL) {
          return packageFolderImage;
        } else if (type == TreeObjectType.BUNDLE) {
          return pluginImage;
        } else if (type == TreeObjectType.REPOSITORY) {
          return reqPluginImage;
        } else if (type == TreeObjectType.REQUIRED_BUNDLES_LABEL) {
          return folderImage;
        } else if (type == TreeObjectType.REQUIRED_BUNDLE) {
          return pluginImage;
        }
      }
      return null;
    }

    public void dispose() {
      pluginImage.dispose();
      reqPluginImage.dispose();
      packageImage.dispose();
      packageFolderImage.dispose();
      folderImage.dispose();
    }

    public Color getBackground(Object element) {
      return Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
    }

    public Color getForeground(Object element) {
      TreeObject node = (TreeObject) element;
      if (!node.isResolved()) {
        if (node.isOptional()) {
          return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
        } else {
          return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
        }
      } else {
        return Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
      }
    }
  }

  class NameSorter extends ViewerSorter {
  }

  public RepositoryView() {
  }

  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, /* SWT.MULTI | */SWT.H_SCROLL | SWT.V_SCROLL);
    drillDownAdapter = new DrillDownAdapter(viewer);
    contentProvider = new ViewContentProvider();
    viewer.setContentProvider(contentProvider);
    viewer.setLabelProvider(new ViewLabelProvider());
    viewer.setSorter(new NameSorter());
    viewer.setInput(getViewSite());
    makeActions();
    hookContextMenu();
    contributeToActionBars();
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        TreeObjectType type = getType(event.getSelection());
        actionAddBundle.setEnabled(type == TreeObjectType.REPOSITORY);
        actionRemoveRepository.setEnabled(type == TreeObjectType.REPOSITORY);
      }
    });
  }

  private TreeObjectType getType(ISelection selection) {
    TreeObject selectedItem = (TreeObject) ((IStructuredSelection) selection).getFirstElement();
    return selectedItem == null ? null : selectedItem.getType();
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        RepositoryView.this.fillContextMenu(manager);
      }
    });
    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    // we don't want other plugin's to contribute
    // getSite().registerContextMenu(menuMgr, viewer);
  }

  private void contributeToActionBars() {
    IActionBars bars = getViewSite().getActionBars();
    fillLocalPullDown(bars.getMenuManager());
    fillLocalToolBar(bars.getToolBarManager());
  }

  private void fillLocalPullDown(IMenuManager manager) {
    manager.add(actionRemoveRepository);
    manager.add(new Separator());
    manager.add(actionAddRepository);
  }

  private void fillContextMenu(IMenuManager manager) {
    TreeObjectType type = getType(viewer.getSelection());
    if (type == TreeObjectType.REPOSITORY) {
      manager.add(actionAddBundle);
      manager.add(actionRemoveRepository);
    } else if (type == TreeObjectType.BUNDLE) {
      manager.add(actionRemoveBundle);
    }
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
    // Other plug-ins can contribute there actions here
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void fillLocalToolBar(IToolBarManager manager) {
    manager.add(actionAddRepository);
    manager.add(actionRemoveRepository);
    manager.add(actionRefresh);
    manager.add(new Separator());
    drillDownAdapter.addNavigationActions(manager);
  }

  private String[] getRepositoryPaths() {
    IEclipsePreferences preferences = new InstanceScope().getNode(Activator.PLUGIN_ID);
    String repositories = preferences.get("repositories", null);
    if (repositories != null) {
      return repositories.split(File.pathSeparator);
    } else {
      return new String[0];
    }
  }

  private void setRepositoryPaths(String[] paths) {
    IEclipsePreferences preferences = new InstanceScope().getNode(Activator.PLUGIN_ID);
    StringBuilder repositories = new StringBuilder();
    for (int i = 0; i < paths.length; i++) {
      if (i != 0) {
        repositories.append(File.pathSeparatorChar);
      }
      repositories.append(paths[i]);
    }
    preferences.put("repositories", repositories.toString());
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      e.printStackTrace();
    }
  }

  private void addRepositoryPath(String path) {
    String[] paths = getRepositoryPaths();
    for (int i = 0; i < paths.length; i++) {
      if (paths[i].equals(path)) {
        return;
      }
    }
    String[] newPaths = new String[paths.length + 1];
    System.arraycopy(paths, 0, newPaths, 0, paths.length);
    newPaths[newPaths.length - 1] = path;
    setRepositoryPaths(newPaths);
  }

  private void removeRepositoryPath(String path) {
    String[] paths = getRepositoryPaths();
    String[] newPaths = new String[paths.length - 1];
    int i;
    int j = 0;
    for (i = 0; i < paths.length; i++) {
      if (!paths[i].equals(path)) {
        newPaths[j++] = paths[i];
      }
    }
    setRepositoryPaths(newPaths);
  }

  private void makeActions() {
    actionAddRepository = new Action() {
      public void run() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setMessage("Choose a repository");
        String directory = dialog.open();
        if (directory != null) {
          File repositoryDirectory = new File(directory);
          BundleRepositoryPersister persister = new BundleRepositoryPersister(repositoryDirectory);
          if (persister.checkRepository()) {
            addRepositoryPath(directory);
            contentProvider.refresh();
            viewer.refresh();
          } else {
            if (!directoryIsEmpty(repositoryDirectory)) {
              if (!MessageDialog.openQuestion(getShell(), "Directory is not empty", 
                  "The selected directory is not empty.\nCreating a repository will delete all files in this directory.\n" +
                  "Are you sure to continue?")) {
                return;
              }
            }
            NewRepositoryDialog newDialog = new NewRepositoryDialog(getShell(), directory);
            if (newDialog.open() == IDialogConstants.OK_ID) {
              Deployer deployer = new Deployer(repositoryDirectory);
              try {
                deployer.create(newDialog.getProfileName());
                addRepositoryPath(directory);
                contentProvider.refresh();
                viewer.refresh();
              } catch (IOException e) {
                MessageDialog.openError(getShell(), "Add repository", "Could not create repository\n" + e.getMessage());
                e.printStackTrace();
              }
            }
          }
        }
      }

      private boolean directoryIsEmpty(File directory) {
        File[] files = directory.listFiles();
        return files == null || files.length == 0;
      }
    };
    actionAddRepository.setText("Add Repository...");
    actionAddRepository.setToolTipText("Add a repository to the view");
    actionAddRepository.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_NEW_WIZARD));

    actionAddBundle = new Action() {
      public void run() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
        dialog.setFilterNames(new String[] { "OSGi Bundle (*.jar)"});
        dialog.setFilterExtensions(new String[] { "*.jar" });
        dialog.setText("Choose a bundle");
        if (dialog.open() != null) {
          ISelection selection = viewer.getSelection();
          TreeObject selectedItem = (TreeObject) ((IStructuredSelection) selection).getFirstElement();
          ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(getShell());
          try {
            String[] files = dialog.getFileNames();
            String path = dialog.getFilterPath();
            for (int i = 0; i < files.length; i++) {
              files[i] = path + File.separatorChar + files[i];
            }
            progressDialog.run(true, false, new BundleDeployJob(getRepositoryLocation(selectedItem), files));
            viewer.refresh();
          } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), "Add bundle", "Could not add bundle\n"
                + e.getTargetException().getMessage());
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    actionAddBundle.setText("Add Bundles...");
    actionAddBundle.setToolTipText("Add bundles to a repository");
    actionAddBundle.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_NEW_WIZARD));

    actionRemoveBundle = new Action() {
      public void run() {
        ISelection selection = viewer.getSelection();
        TreeObject selectedItem = (TreeObject) ((IStructuredSelection) selection).getFirstElement();
        BundleDescriptor descriptor = (BundleDescriptor) selectedItem.getValue();
        if (MessageDialog.openConfirm(getShell(), "Removing bundle", "Are you sure to remove bundle\n"
            + descriptor.getBundleSymbolicName() + " (" + descriptor.getBundleVersion() + ")")) {
          ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(getShell());
          try {
            progressDialog.run(true, false, new BundleUndeployJob(getRepositoryLocation(selectedItem.getParent()), 
                descriptor.getBundleSymbolicName(), descriptor.getBundleVersion()));
            viewer.refresh();
          } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), "Add bundle", "Could not remove bundle\n"
                + e.getTargetException().getMessage());
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    actionRemoveBundle.setText("Remove Bundle...");
    actionRemoveBundle.setToolTipText("Removes a bundle from the repository");
    actionRemoveBundle.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_DELETE));

    actionRemoveRepository = new Action() {
      public void run() {
        ISelection selection = viewer.getSelection();
        Object selectedItem = ((IStructuredSelection) selection).getFirstElement();
        if (MessageDialog.openConfirm(getShell(), "Removing repository", "Are you sure to remove repository\n"
            + selectedItem)) {
          removeRepositoryPath(getRepositoryLocation((TreeObject) selectedItem).toString());
          contentProvider.refresh();
          viewer.refresh();
        }
      }
    };
    actionRemoveRepository.setText("Remove Repository...");
    actionRemoveRepository.setToolTipText("Removes a repository from the view");
    actionRemoveRepository.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
        ISharedImages.IMG_TOOL_DELETE));

    actionRefresh = new Action() {
      public void run() {
        contentProvider.refresh();
        viewer.refresh();
      }
    };
    actionRefresh.setText("Refresh Repository View");
    actionRefresh.setToolTipText("Refreshes the repository view");
    actionRefresh.setImageDescriptor(Activator.getImageDescriptor("icons/refresh_obj.gif"));
  }

  private File getRepositoryLocation(TreeObject selectedItem) {
    if (selectedItem.getValue() instanceof BundleRepository) {
      return ((BundleRepository)selectedItem.getValue()).getLocation();
    } else {
      return new File(selectedItem.getValue().toString());
    }
  }

  private Shell getShell() {
    return viewer.getControl().getShell();
  }

  public void setFocus() {
    viewer.getControl().setFocus();
  }

  private class BundleDeployJob implements IRunnableWithProgress {
    private final String[] bundleJars;
    private final File repositoryDirectory;

    private BundleDeployJob(File repositoryDirectory, String[] bundleJars) {
      this.repositoryDirectory = repositoryDirectory;
      this.bundleJars = bundleJars;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      Deployer deployer = new Deployer(repositoryDirectory);
      try {
        for (String bundleJar : bundleJars) {
          deployer.deploy(new File(bundleJar));
        }
        contentProvider.refresh();
      } catch (IOException e) {
        throw new InvocationTargetException(e);
      }
    }
  }

  private class BundleUndeployJob implements IRunnableWithProgress {
    private final File repositoryDirectory;
    private String bundleSymbolicName;
    private Version bundleVersion;

    private BundleUndeployJob(File repositoryDirectory, String bundleSymbolicName, Version bundleVersion) {
      this.repositoryDirectory = repositoryDirectory;
      this.bundleSymbolicName = bundleSymbolicName;
      this.bundleVersion = bundleVersion;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      Deployer deployer = new Deployer(repositoryDirectory);
      try {
        deployer.undeploy(bundleSymbolicName, bundleVersion, false);
        contentProvider.refresh();
      } catch (IOException e) {
        throw new InvocationTargetException(e);
      }
    }
  }
}