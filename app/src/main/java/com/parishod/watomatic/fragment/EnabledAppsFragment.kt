package com.parishod.watomagic.fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.NonNull
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.parishod.watomagic.adapter.SupportedAppsAdapter
import com.parishod.watomagic.model.App
import com.parishod.watomagic.model.utils.Constants

import com.parishod.watomagic.databinding.FragmentEnabledAppsBinding

class EnabledAppsFragment : Fragment() {

    private var _binding: FragmentEnabledAppsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEnabledAppsBinding.inflate(inflater, container, false)

        val layoutManager = LinearLayoutManager(context)

        val supportedAppsAdapter = SupportedAppsAdapter(Constants.EnabledAppsDisplayType.VERTICAL, ArrayList<App>(Constants.SUPPORTED_APPS), null)
        binding.supportedAppsList.layoutManager = layoutManager
        binding.supportedAppsList.adapter = supportedAppsAdapter

        return binding.root
    }
}