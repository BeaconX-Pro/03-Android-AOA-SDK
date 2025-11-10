package com.moko.bxp.a.c.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * @author: jun.liu
 * @date: 2024/10/29 10:19
 * @des:
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {
    protected VB mBind;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBind = getViewBind(inflater, container);
        onCreateView();
        return mBind.getRoot();
    }

    protected abstract VB getViewBind(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    protected void onCreateView() {
    }
}
