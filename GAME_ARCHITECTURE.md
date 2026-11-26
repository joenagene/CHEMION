
# CHEMION: The Last Cure
## Game Architecture

### 1. Main Menu
- Player opens CHEMION: The Last Cure.
- Main Menu appears with background music and interactive buttons.
- Options: **New Game, Continue Game, Character Selection, Game Instructions, and Exit Game**.
- Selecting New Game opens Character Selection.
- Continue Game opens the level selection screen.
- Players can access unlocked levels and resume saved progress.

### 2. Character Selection
- Player chooses one of the five available scientists:
  - Dr. Sebastian
  - Dr. Dutch
  - Dr. Nicole
  - Dr. Gene
  - Dr. Elena
- Each scientist has a unique appearance and character description.
- Each scientist has individual sprite animations.
- Player confirms the selected scientist.
- The opening cinematic and scientist conversation appear before entering Level 1.
- The selected scientist remains consistent throughout all five levels.

### 3. Level 1 — The Outbreak (Abandoned Town)
- Player enters an explorable abandoned town.
- Player moves using WASD while the camera follows.
- Player explores streets, buildings, and contaminated areas.
- Zombies appear progressively through multiple waves.
- Enemy encounters introduce information about the infection and possible treatments.
- Player uses available chemicals, potions, and supplies to survive.

**Enemies:**
- Common Zombie
- Runner Zombie
- Crawler Zombie
- Mutated Survivor (Boss)

**Zombie Encounter:**
- Zombies detect and chase the player.
- First encounters reveal information about the zombie.
- Player investigates the zombie's condition and chemical weaknesses.
- Player prepares and throws the appropriate potion.
- Successful encounters provide rewards such as experience points and coins.

**Potion Mixing:**
- Player accesses the Chemistry Laboratory.
- Player examines available ingredients and research clues.
- Player selects the appropriate combination of chemicals.
- Correct combinations produce the required potion.
- Incorrect combinations result in unsuccessful crafting.
- Successfully crafted potions are stored for combat.

**Level Completion:**
- Complete all required zombie waves.
- Defeat the Mutated Survivor.
- Complete the ending conversation.
- Level 2 becomes available.

### 4. Level 2 — The Contaminated Hospital
- An opening cinematic introduces the hospital and its infected patients.
- Player explores contaminated hospital areas.
- Stronger zombies appear with different movement and attack patterns.
- Player investigates infection conditions and chemical weaknesses.
- Player creates appropriate potions using available ingredients.
- Combat difficulty increases as the player progresses.

**Enemies:**
- Acid Zombie
- Patient Zero
- Toxic Zombie
- Plague Doctor (Boss)

**Level Completion:**
- Complete the required zombie waves.
- Defeat the Plague Doctor.
- Complete the ending conversation.
- Unlock Level 3.

### 5. Level 3 — The Toxic Chemical Factory
- An opening cinematic introduces the contaminated factory.
- Player explores industrial areas containing chemical equipment and obstacles.
- Mutated zombies appear with increased combat difficulty.
- Player investigates the effects of chemical contamination.
- Chemical clues guide the selection of appropriate potion ingredients.
- Player prepares stronger potions for advanced enemies.

**Enemies:**
- Toxic Brute
- Venom Spitter
- Explosive Zombie
- Chemical Abomination (Boss)

**Level Completion:**
- Complete the required zombie waves.
- Defeat the Chemical Abomination.
- Complete the ending conversation.
- Unlock Level 4.

### 6. Level 4 — The Lost Research Facility
- Player enters an abandoned experimental research facility.
- The environment contains specimen tanks, computers, research documents, and damaged laboratory equipment.
- Player investigates the facility to uncover information about the outbreak.
- Research clues reveal the origins and conditions of experimental zombies.
- Advanced enemies require more complex chemical combinations.
- Laboratory equipment and specimen tanks serve as movement obstacles.
- Visual effects create a dangerous laboratory atmosphere.

**Enemies:**
- Experiment 01
- Mutant Stalker
- Project Chimera
- Regenerator Zombie (Boss)

**Advanced Potion Mixing:**
- The first zombie requires a three-ingredient potion.
- The second and third zombies require four-ingredient potions.
- The boss requires a five-ingredient potion.
- Research clues explain the purpose of the required ingredients.

**Level Completion:**
- Investigate the research facility.
- Complete all required zombie waves.
- Defeat the Regenerator Zombie.
- Complete the ending conversation.
- Unlock Level 5.

### 7. Level 5 — The Final Cure (Genesis Laboratory)
- Player enters Genesis Laboratory, where the outbreak originated.
- The laboratory contains multiple interconnected rooms.
- Each room has a distinct environment, layout, and research information.
- Player investigates classified records and experimental subjects.
- Progressive zombie waves increase combat difficulty.
- Sliding doors provide access to succeeding laboratory areas.
- Players must complete the required encounters before progressing.

**Enemies:**
- Elite Infected
- Biohazard Guardian
- Alpha Mutant
- Patient Zero: The Origin (Final Boss)

**Laboratory Exploration:**
- Player explores the Presentation Hall.
- Player progresses into the Containment Vault.
- Player investigates the Evolution Wing.
- Player discovers information about the Genesis experiments.
- Player encounters a chemically sealed entrance to the final laboratory.
- Research clues guide the creation of a chemical solution to remove the seal.
- Successfully removing the seal grants access to the Origin Vault.

**Final Boss Encounter:**
- A cinematic reveals the final containment facility.
- The final boss appears with a red warning effect.
- Patient Zero: The Origin requires two different specialized potions.
- The first potion requires five ingredients.
- The second potion requires six ingredients.
- Player must successfully use the required potions while avoiding enemy attacks.
- Defeating the final boss completes the final mission.

### 8. Zombie Combat System
- Zombies appear progressively through enemy waves.
- Zombies detect and chase nearby players.
- Different zombie types have different movement speeds, health, damage, and weaknesses.
- Zombie attacks reduce the player's health.
- Player can walk, sprint, evade enemies, and throw potions.
- Correct potions damage or neutralize their intended zombie targets.
- Successful encounters provide experience points, coins, and other rewards.
- Hurt effects provide visual feedback during combat.
- Boss encounters include warning effects and increased difficulty.
- Completing an enemy wave allows the player to progress.

### 9. Health and Death System
- Player begins with a set amount of health points (HP).
- Enemy attacks reduce the player's HP.
- A health bar displays the player's current condition.
- Recovery items restore lost health.
- Visual damage effects indicate successful enemy attacks.
- When HP reaches zero, the player is defeated.
- The Game Over interface provides options for continuing or leaving the game.

### 10. Inventory System
- Stores available chemicals, potions, and recovery items.
- Tracks ingredient and item quantities.
- Prevents crafting when required ingredients are insufficient.
- Removes ingredients consumed during potion mixing.
- Stores successfully crafted potions.
- Allows players to select and use available combat and recovery items.
- Updates inventory quantities after purchases, crafting, and item usage.

### 11. Chemistry Laboratory System
- Provides an interface for preparing chemical potions.
- Displays available ingredients and their quantities.
- Allows players to select and combine ingredients.
- Uses research clues to guide chemical selection.
- Validates ingredient combinations against potion recipes.
- Correct combinations produce the appropriate potion.
- Incorrect combinations result in unsuccessful crafting.
- Potion recipes become more complex in succeeding levels.
- Crafted potions are used against specific zombie types.

### 12. Research Journal System
- Stores information about encountered zombies.
- Displays zombie descriptions, infection conditions, and weaknesses.
- Provides research notes and chemical clues.
- Helps players identify appropriate potion ingredients.
- Records discovered potion recipes.
- Updates research information as the player progresses.
- Supports chemistry-based problem-solving during gameplay.

### 13. Economy and Reward System
- Players earn coins and experience points through combat.
- Coins can be used to purchase chemicals and available supplies.
- Purchased ingredients are added to the inventory.
- Ingredient quantities and prices are displayed during purchasing.
- Quantity selection allows players to purchase multiple items.
- Rewards and available resources support progression through increasingly difficult encounters.

### 14. Movement and Collision System
- Player moves using WASD controls.
- Sprinting allows faster movement during combat and exploration.
- The camera follows the player's movement across larger battlefields.
- Walls, furniture, laboratory equipment, and specimen tanks restrict movement.
- Collision detection prevents players from passing through solid obstacles.
- Door interaction allows progression between designated areas.
- Directional sprite animations respond to player and enemy movement.

### 15. Animation and Audio System
- Character sprites support directional movement animations.
- Zombie sprites support movement and attack animations.
- Potion throwing and combat effects provide visual feedback.
- Damage effects indicate when characters or enemies are hurt.
- Boss warning effects emphasize major encounters.
- Cinematic transitions introduce important story events.
- Background music, ambient audio, and sound effects enhance gameplay.
- Audio management controls music and sound playback across game screens.

### 16. Checkpoint and Progression System
- Game progress is stored using a persistent save system.
- Saved data records the selected scientist and level progression.
- Battlefield checkpoints preserve supported gameplay information.
- Continue Game allows players to resume saved progress.
- Completed levels remain unlocked.
- Completing a level unlocks the next location.
- Players can access previously unlocked levels through Level Selection.
- Starting a new game resets existing progression after confirmation.

### 17. Pause and Navigation System
- Players can pause gameplay through the Pause interface.
- Gameplay is temporarily suspended while paused.
- Players can resume gameplay or return to the main menu.
- Save and exit options allow players to preserve their progress.
- Screen transitions connect menus, cinematics, battlefields, and ending sequences.

### 18. Victory and Ending
- Player defeats Patient Zero: The Origin.
- The original infection strain is neutralized.
- The final victory sequence appears.
- The scientist completes the final ending conversation.
- The scientist discovers classified Genesis research records.
- The records reveal that Genesis deliberately conducted human mutation experiments.
- Additional research facilities and experimental subjects are discovered.
- An unidentified voice interrupts the scientist.
- The screen transitions into darkness.
- A mysterious underground laboratory is revealed.
- Two unidentified individuals discuss the fall of Genesis and the next phase of their experiments.
- An unidentified specimen inside a containment tank begins to move.
- The cinematic concludes with the CHEMION: The Last Cure title.
- The player returns to the Level Selection screen.

### 19. Technical Architecture
- Java serves as the primary programming language.
- Java Swing manages graphical interfaces, menus, and game screens.
- Java AWT and Graphics2D handle battlefield rendering, animations, and visual effects.
- CardLayout manages transitions between different game screens.
- Swing timers manage gameplay updates and cinematic animations.
- Object-oriented programming organizes characters, enemies, items, recipes, and game systems.
- Separate battlefield classes manage the gameplay of each level.
- Separate cinematic classes manage opening stories and ending sequences.
- The OutbreakEnding class manages the final cinematic.
- The Main class coordinates screen navigation and game progression.
- The SaveManager class manages persistent game data.
- The AudioManager class manages music and sound effects.

---

### Overall Game Flow

**Launch Game → Main Menu → New Game → Character Selection → Opening Cinematic → Scientist Conversation → Enter Battlefield → Explore → Encounter Zombies → Investigate Research Clues → Mix Potions → Engage in Combat → Earn Rewards → Complete Enemy Waves → Defeat Level Boss → Ending Conversation → Unlock Next Level → Repeat for Levels 2–5 → Enter Origin Vault → Defeat Patient Zero: The Origin → Victory Sequence → Final Ending Conversation → Classified Records Discovery → Mysterious Laboratory Cinematic → CHEMION Title Reveal → Level Selection**

**Continue Game Flow:** Main Menu → Continue Game → Level Selection → Select Unlocked Level → Restore Saved Progress or Begin Level.

**Failure Flow:** Zombie Attack → HP Reaches Zero → Game Over → Retry or Return to Main Menu.

**Crafting Flow:** Open Chemistry Laboratory → Examine Research Clues → Select Ingredients → Validate Combination → Craft Potion → Update Inventory → Use Potion in Combat.
