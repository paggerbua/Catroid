/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2018 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.test.content.controller;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.io.ResourceImporter;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.ui.controller.BackpackListManager;
import org.catrobat.catroid.ui.recyclerview.controller.SceneController;
import org.catrobat.catroid.utils.PathBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;

import static org.catrobat.catroid.common.Constants.BACKPACK_SCENE_DIRECTORY;
import static org.catrobat.catroid.common.Constants.IMAGE_DIRECTORY_NAME;
import static org.catrobat.catroid.common.Constants.SOUND_DIRECTORY_NAME;
import static org.catrobat.catroid.uiespresso.util.FileTestUtils.assertFileDoesNotExist;
import static org.catrobat.catroid.uiespresso.util.FileTestUtils.assertFileExists;
import static org.catrobat.catroid.utils.PathBuilder.buildProjectPath;
import static org.catrobat.catroid.utils.PathBuilder.buildScenePath;

@RunWith(AndroidJUnit4.class)
public class SceneControllerTest {

	private Project project;
	private Scene scene;

	@Before
	public void setUp() throws IOException {
		clearBackPack();
		createProject();
	}

	@After
	public void tearDown() throws IOException {
		deleteProject();
		clearBackPack();
	}

	@Test
	public void testRenameScene() {
		String previousName = scene.getName();
		String newName = "new Scene Name";

		SceneController controller = new SceneController();
		controller.rename(scene, newName);

		assertEquals(newName, scene.getName());
		assertEquals(new File(PathBuilder.buildScenePath(project.getName(), newName)), scene.getDirectory());

		assertFileDoesNotExist(new File(PathBuilder.buildScenePath(project.getName(), previousName)));
		assertFileExists(new File(PathBuilder.buildScenePath(project.getName(), newName)));
	}

	@Test
	public void testCopyScene() throws IOException {
		SceneController controller = new SceneController();
		Scene copy = controller.copy(scene, project);

		assertEquals(1, project.getSceneList().size());
		assertEquals(scene.getSpriteList().size(), copy.getSpriteList().size());

		for (int i = 0; i < copy.getSpriteList().size(); i++) {
			assertEquals(
					scene.getSpriteList().get(i).getLookList().size(),
					copy.getSpriteList().get(i).getLookList().size());

			assertEquals(
					scene.getSpriteList().get(i).getSoundList().size(),
					copy.getSpriteList().get(i).getSoundList().size());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfScripts(),
					copy.getSpriteList().get(i).getNumberOfScripts());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfBricks(),
					copy.getSpriteList().get(i).getNumberOfBricks());
		}

		assertLookFileExistsInScene(copy.getSpriteList().get(1).getLookList().get(0).getFile().getName(), copy);
		assertSoundFileExistsInScene(copy.getSpriteList().get(1).getSoundList().get(0).getFile().getName(), copy);
	}

	@Test
	public void testDeleteScene() throws IOException {
		SceneController controller = new SceneController();
		File deletedSceneDirectory = scene.getDirectory();

		controller.delete(scene);

		assertEquals(1, project.getSceneList().size());
		assertFileDoesNotExist(deletedSceneDirectory);
	}

	@Test
	public void testPackScene() throws IOException {
		SceneController controller = new SceneController();
		Scene packedScene = controller.pack(scene);

		assertEquals(0, BackpackListManager.getInstance().getScenes().size());

		assertEquals(new File(BACKPACK_SCENE_DIRECTORY, packedScene.getName()), packedScene.getDirectory());
		assertFileExists(packedScene.getDirectory());

		assertEquals(scene.getSpriteList().size(), packedScene.getSpriteList().size());

		for (int i = 0; i < packedScene.getSpriteList().size(); i++) {
			assertEquals(
					scene.getSpriteList().get(i).getLookList().size(),
					packedScene.getSpriteList().get(i).getLookList().size());

			assertEquals(
					scene.getSpriteList().get(i).getSoundList().size(),
					packedScene.getSpriteList().get(i).getSoundList().size());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfScripts(),
					packedScene.getSpriteList().get(i).getNumberOfScripts());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfBricks(),
					packedScene.getSpriteList().get(i).getNumberOfBricks());
		}

		assertLookFileExistsInScene(
				packedScene.getSpriteList().get(1).getLookList().get(0).getFile().getName(),
				packedScene);

		assertSoundFileExistsInScene(
				packedScene.getSpriteList().get(1).getSoundList().get(0).getFile().getName(),
				packedScene);
	}

	@Test
	public void testUnpackScene() throws IOException {
		SceneController controller = new SceneController();

		Scene packedScene = controller.pack(scene);
		Scene unpackedScene = controller.unpack(packedScene, project);

		assertEquals(0, BackpackListManager.getInstance().getScenes().size());

		assertEquals(1, project.getSceneList().size());

		assertEquals(scene.getSpriteList().size(), unpackedScene.getSpriteList().size());

		for (int i = 0; i < unpackedScene.getSpriteList().size(); i++) {
			assertEquals(
					scene.getSpriteList().get(i).getLookList().size(),
					unpackedScene.getSpriteList().get(i).getLookList().size());

			assertEquals(
					scene.getSpriteList().get(i).getSoundList().size(),
					unpackedScene.getSpriteList().get(i).getSoundList().size());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfScripts(),
					unpackedScene.getSpriteList().get(i).getNumberOfScripts());

			assertEquals(
					scene.getSpriteList().get(i).getNumberOfBricks(),
					unpackedScene.getSpriteList().get(i).getNumberOfBricks());
		}

		assertLookFileExistsInScene(
				unpackedScene.getSpriteList().get(1).getLookList().get(0).getFile().getName(),
				unpackedScene);

		assertSoundFileExistsInScene(
				unpackedScene.getSpriteList().get(1).getSoundList().get(0).getFile().getName(),
				unpackedScene);
	}

	private void assertLookFileExistsInScene(String fileName, Scene scene) {
		assertFileExists(new File(new File(scene.getDirectory(), Constants.IMAGE_DIRECTORY_NAME), fileName));
	}

	private void assertSoundFileExistsInScene(String fileName, Scene scene) {
		assertFileExists(new File(new File(scene.getDirectory(), Constants.SOUND_DIRECTORY_NAME), fileName));
	}

	private void clearBackPack() throws IOException {
		if (BACKPACK_SCENE_DIRECTORY.exists()) {
			StorageOperations.deleteDir(BACKPACK_SCENE_DIRECTORY);
		}
		BACKPACK_SCENE_DIRECTORY.mkdirs();
	}

	private void createProject() throws IOException {
		project = new Project(InstrumentationRegistry.getTargetContext(), "SpriteControllerTest");
		scene = project.getDefaultScene();
		ProjectManager.getInstance().setCurrentProject(project);

		Sprite sprite = new Sprite("testSprite");
		scene.addSprite(sprite);

		StartScript script = new StartScript();
		PlaceAtBrick placeAtBrick = new PlaceAtBrick(0, 0);
		script.addBrick(placeAtBrick);
		sprite.addScript(script);

		XstreamSerializer.getInstance().saveProject(project);

		File imageFile = ResourceImporter.createImageFileFromResourcesInDirectory(
				InstrumentationRegistry.getContext().getResources(),
				org.catrobat.catroid.test.R.raw.red_image,
				new File(project.getDefaultScene().getDirectory(), IMAGE_DIRECTORY_NAME),
				"red_image.png",
				1);

		sprite.getLookList().add(new LookData("testLook", imageFile));

		File soundFile = ResourceImporter.createSoundFileFromResourcesInDirectory(
				InstrumentationRegistry.getContext().getResources(),
				org.catrobat.catroid.test.R.raw.longsound,
				new File(buildScenePath(project.getName(), project.getDefaultScene().getName()), SOUND_DIRECTORY_NAME),
				"longsound.mp3");

		sprite.getSoundList().add(new SoundInfo("testSound", soundFile));

		XstreamSerializer.getInstance().saveProject(project);
	}

	private void deleteProject() throws IOException {
		File projectDir = new File(buildProjectPath(project.getName()));
		if (projectDir.exists()) {
			StorageOperations.deleteDir(projectDir);
		}
	}
}
