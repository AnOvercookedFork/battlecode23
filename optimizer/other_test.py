# runs all maps for two bots

import os
import time
import signal
import random

# these constants are probably fine
maps = ['AbsoluteW', 'Buggy', 'BuildSite', 'Cave', 'Cee', 'CrownJewels', 'Elephant', 'ExtremelyMid', 'FishCake', 'Fractured', 'Heart', 'HotAirBalloon', 'IslandHoppingTwo', 'LightWork', 'LookingGlass', 'MassiveL', 'MoonPhases', 'Pillars', 'PipesAndParabolas', 'Potions', 'Rainbow', 'Resign', 'ReverseFunnel', 'Sneaky', 'Spiderweb', 'Spots', 'Swooshy', 'Target', 'Tightrope', 'VeryReasonable', 'Zig', 'BatSignal']
random.shuffle(maps)
# set these
teamA = 'klein_bottle2_03_1'
teamB = 'mittens'

def get_winner(data):
    # does weird stuff if client is open, opened, or closed
    result = data[-5].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    result = data[-6].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    return data[-7].split(' ')[-4][1:2]

def run_matches(teamA, teamB, score):
    map_score = {}
    for map in maps:
        current = time.time()
        result = os.popen('gradlew run -Pmaps={map} -PteamA={a} -PteamB={b} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'.format(map = map, a = teamA, b = teamB)).read().splitlines()
        print('Last round {} took {} seconds'.format(map, time.time() - current))
        winner = get_winner(result)
        if winner == 'A':
            score[0] += 1
            map_score[map] = teamA
        elif winner == 'B':
            score[1] += 1
            map_score[map] = teamB
        else:
            print('I don\'t know who won this, take a look: {}'.format(result))
        
        result = os.popen('gradlew run -Pmaps={map} -PteamA={b} -PteamB={a} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false'.format(map = map, a = teamA, b = teamB)).read().splitlines()
        if winner == 'A':
            score[1] += 1
            if map_score[map] == teamB:
                print('{} won both sides on {}'.format(teamB, map))
        elif winner == 'B':
            score[0] += 1
            if map_score[map] == teamA:
                print('{} won both sides on {}'.format(teamA, map))
        else:
            print('I don\'t know who won this, take a look: {}'.format(result))
        
    return score, map_score

def handle_sigint():
    exit()

def main():
    os.chdir('..')
    os.system('gradlew update')
    os.system('gradlew build')

    current = time.time()
    score1 = run_matches(teamA, teamB, [0, 0])
    print('Match 1 score ({} vs {}): {}'.format(teamA, teamB, score1[0]))
    # print('Match 1 maps: {}'.format(score1[1]))
    # score2 = run_matches(teamB, teamA, [0, 0])
    # print('Match 2 score ({} vs {}): {}'.format(teamB, teamA, score2[0]))
    # print('Match 2 maps: {}'.format(score2[1]))
    # final_score = [score1[0][0] + score2[0][1], score1[0][1] + score2[0][0]]
    # print('Final score ({} vs {}): {}'.format(teamA, teamB, final_score))

    # for key in score1[1].keys():
    #     if score1[1][key] == score2[1][key]:
    #         print('{} won both sides on {}'.format(score1[1][key], key))

    print('Total runtime was {} seconds'.format(time.time() - current))

if __name__ == '__main__':
    main()