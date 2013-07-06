package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.totschnig.myexpenses.model.ContribFeature.Feature;

public interface ContribIFace {

  /**
   * @param feature
   * called when the user clicks on "not yet", and calls the requested feature
   * @param tag TODO
   */
  public abstract void contribFeatureCalled(Feature feature, Serializable tag);

  /**
   * the user can either click on "Buy" or cancel the dialog
   * for the moment, we are fine with the same callback for both cases,
   * for example, in some cases, the calling activity might have to be finished
   */
  public abstract void contribFeatureNotCalled();

}