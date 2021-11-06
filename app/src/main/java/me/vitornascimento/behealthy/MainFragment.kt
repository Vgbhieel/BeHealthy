package me.vitornascimento.behealthy

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.vitornascimento.behealthy.databinding.FragmentMainBinding

class MainFragment : Fragment(R.layout.fragment_main) {

  private var _binding: FragmentMainBinding? = null
  private val binding get() = _binding!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    _binding = FragmentMainBinding.bind(view)
    initializeView()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun initializeView() {
    val adapter = ArrayAdapter(
      requireContext(),
      R.layout.hospital_categories_list_item,
      resources.getStringArray(R.array.hospital_categories)
    )

    with(binding.dropdownMenu.editText as AutoCompleteTextView) {
      setAdapter(adapter)
      setOnItemClickListener { _, _, _, _ ->
        binding.btnFindHospital.isEnabled = true
      }
    }

    binding.btnFindHospital.setOnClickListener {
      (binding.dropdownMenu.editText as AutoCompleteTextView).text.toString().let {
        findNavController().navigate(MainFragmentDirections.actionMainFragmentToHospitalResultFragment(it))
      }
    }
  }

}
