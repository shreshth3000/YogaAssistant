package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.yogakotlinpipeline.app.databinding.FragmentInside2Binding

class Inside2Fragment : Fragment() {

    private var _binding: FragmentInside2Binding? = null
    private val binding get() = _binding!!
    
    private lateinit var asanaAdapter: YogaAsanaAdapter
    private var currentAsanas = YogaAsanaDataProvider.allAsanas
    // ADDED: Debouncing handler for search to prevent ANR
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupFilterTabs()
    }

    private fun setupClickListeners() {
        // Menu Button
        binding.btnMenu.setOnClickListener {
            android.widget.Toast.makeText(context, "Menu", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Search Header Button
        binding.btnSearch.setOnClickListener {
            binding.etSearch.requestFocus()
        }

        // Search functionality
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // CHANGED: Add debouncing to prevent ANR during rapid typing
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    performSearch(s.toString())
                }
                searchHandler.postDelayed(searchRunnable!!, 300) // 300ms debounce
            }
        })

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside2Fragment_to_inside1Fragment)
        }

        binding.btnExplore.setOnClickListener {
            // Already on explore screen, do nothing
        }
        
        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_inside2Fragment_to_inside3Fragment)
        }

        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_inside2Fragment_to_inside4Fragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside2Fragment_to_profileFragment)
        }
    }

    private fun setupRecyclerView() {
        // CHANGED: Remove Context parameter to prevent Activity leak
        asanaAdapter = YogaAsanaAdapter(currentAsanas) { asana ->
            // Navigate to YouTube video flow with asana information
            val bundle = Bundle().apply {
                putString("pose_name", asana.sanskritName)
                putString("pose_display_name", asana.name)
                putString("pose_description", asana.description)
                putString("pose_difficulty", asana.difficultyLevel.name.lowercase().replaceFirstChar { it.uppercase() })
            }

            try {
                findNavController().navigate(R.id.action_exploreFragment_to_youtubeVideoFragment, bundle)
            } catch (e: Exception) {
                // Fallback directly to the fragment id if action not resolved
                try {
                    findNavController().navigate(R.id.youtubeVideoFragment, bundle)
                } catch (ex: Exception) {
                    android.widget.Toast.makeText(requireContext(), "Navigation failed: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        
        binding.rvAsanaList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = asanaAdapter
        }
    }

    private fun performSearch(query: String) {
        val filteredAsanas = YogaAsanaDataProvider.searchAsanas(query)
        currentAsanas = filteredAsanas
        asanaAdapter.updateAsanas(filteredAsanas)
    }

    private fun showAllAsanas() {
        currentAsanas = YogaAsanaDataProvider.allAsanas
        asanaAdapter.updateAsanas(currentAsanas)
    }

    private fun filterAsanas(query: String) {
        val filteredAsanas = YogaAsanaDataProvider.searchAsanas(query)
        currentAsanas = filteredAsanas
        asanaAdapter.updateAsanas(filteredAsanas)
    }
    
    private fun filterAsanasByLevel(level: DifficultyLevel) {
        val filteredAsanas = YogaAsanaDataProvider.getAsanasByLevel(level)
        currentAsanas = filteredAsanas
        asanaAdapter.updateAsanas(filteredAsanas)
    }

    private fun setupFilterTabs() {
        // All tab is already active by default
        binding.tabAll.setOnClickListener {
            updateFilterTabs(binding.tabAll)
            showAllAsanas()
        }

        binding.tabBeginner.setOnClickListener {
            updateFilterTabs(binding.tabBeginner)
            filterAsanasByLevel(DifficultyLevel.BEGINNER)
        }

        binding.tabIntermediate.setOnClickListener {
            updateFilterTabs(binding.tabIntermediate)
            filterAsanasByLevel(DifficultyLevel.INTERMEDIATE)
        }

        binding.tabAdvanced.setOnClickListener {
            updateFilterTabs(binding.tabAdvanced)
            filterAsanasByLevel(DifficultyLevel.ADVANCED)
        }

        binding.tabRestorative.setOnClickListener {
            updateFilterTabs(binding.tabRestorative)
            // For now, show all asanas for restorative
            showAllAsanas()
        }
    }

    private fun updateFilterTabs(activeTab: android.widget.TextView) {
        // Reset all tabs to inactive
        binding.tabAll.background = resources.getDrawable(R.drawable.inactive_filter_background, null)
        binding.tabAll.setTextColor(resources.getColor(R.color.text_primary, null))
        
        binding.tabBeginner.background = resources.getDrawable(R.drawable.inactive_filter_background, null)
        binding.tabBeginner.setTextColor(resources.getColor(R.color.text_primary, null))
        
        binding.tabIntermediate.background = resources.getDrawable(R.drawable.inactive_filter_background, null)
        binding.tabIntermediate.setTextColor(resources.getColor(R.color.text_primary, null))
        
        binding.tabAdvanced.background = resources.getDrawable(R.drawable.inactive_filter_background, null)
        binding.tabAdvanced.setTextColor(resources.getColor(R.color.text_primary, null))
        
        binding.tabRestorative.background = resources.getDrawable(R.drawable.inactive_filter_background, null)
        binding.tabRestorative.setTextColor(resources.getColor(R.color.text_primary, null))

        // Set active tab
        activeTab.background = resources.getDrawable(R.drawable.active_filter_background, null)
        activeTab.setTextColor(resources.getColor(R.color.white, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ADDED: Cancel pending search operations to prevent memory leaks
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        searchRunnable = null
        _binding = null
    }
}

