package com.example.madlevel4example

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.madlevel4example.databinding.FragmentRemindersBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private lateinit var reminderRepository: ReminderRepository

    private val reminders = arrayListOf<Reminder>()
    private val reminderAdapter = ReminderAdapter(reminders)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()

        observeAddReminderResult()

        reminderRepository = ReminderRepository(requireContext())
        getRemindersFromDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getRemindersFromDatabase() {
        CoroutineScope(Dispatchers.Main).launch {
            val reminders = withContext(Dispatchers.IO) {
                reminderRepository.getAllReminders()
            }
            this@RemindersFragment.reminders.clear()
            this@RemindersFragment.reminders.addAll(reminders)
            reminderAdapter.notifyDataSetChanged()
        }
    }

    private fun initViews() {
        // Initialize the recycler view with a linear layout manager, adapter
        binding.rvReminders.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.rvReminders.adapter = reminderAdapter

        createItemTouchHelper().attachToRecyclerView(binding.rvReminders)
    }

    private fun observeAddReminderResult() {
        setFragmentResultListener(REQ_REMINDER_KEY) { key, bundle ->
            bundle.getString(BUNDLE_REMINDER_KEY)?.let {
                val reminder = Reminder(it)

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        reminderRepository.insertReminder(reminder)
                    }
                    getRemindersFromDatabase()
                }
            } ?: Log.e("ReminderFragment", "Request triggered, but empty reminder text!")
        }
    }

    private fun createItemTouchHelper(): ItemTouchHelper {

        // Callback which is used to create the ItemTouch helper. Only enables left swipe.
        // Use ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) to also enable right swipe.
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // Enables or Disables the ability to move items up and down.
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            // Callback triggered when a user swiped an item.
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val reminderToDelete = reminders[position]

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        reminderRepository.deleteReminder(reminderToDelete)
                    }
                    getRemindersFromDatabase()
                }
            }
        }
        return ItemTouchHelper(callback)
    }
}