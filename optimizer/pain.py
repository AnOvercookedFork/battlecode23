# runs all maps for two bots

import os
import time
import subprocess
import signal

# these constants are probably fine
maps = ['AbsoluteW','AllElements','ArtistRendition','Barcode','BatSignal','BattleSuns','BowAndArrow','Buggy','Cat','Cave','Cee','Checkmate2','Clown','Contraction','Cornucopia','Crossword','Cube','DefaultMap','Diagonal','Divergence','Dreamy','Eyelands','Flower','Forest','FourNations','Frog','Grapes','Grievance','Hah','Heart','HideAndSeek','HorizontallySymmetric','HotAirBalloon','IslandHopping','IslandHoppingTwo','Jail','KingdomRush','Lantern','LightWork','Lines','maptestsmall','Marsh','MassiveL','Maze','Minefield','Movepls','Orbit','PairedProgramming','Pakbot','Pathfind','Piglets','Pit','Pizza','Potions','Quiet','RaceToTheTop','Rainbow','Rectangle','Repetition','Resign','Rewind','Risk','River','RockWall','Sakura','Scatter','Sine','SmallElements','Sneaky','Snowflake','SomethingFishy','SoundWave','Spin','Spiral','Squares','Star','Sun','Sus','SweetDreams','Tacocat','Target','ThirtyFive','TicTacToe','Tightrope','TimesUp','TreasureMap','Turtle','USA','VerticallySymmetric']
max_subprocesses = 12

# set these
teamA = 'klein_bottle2_02_2'
teamB = 'quals'


# global vars (cuz im lazy)
processes = set()
current_score = []
current_map_score = {}
curTA = ''
curTB = ''

def get_winner(data):
    print(data)
    # does weird stuff if client is open, opened, or closed
    result = data[-5].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    result = data[-6].split(' ')[-4][1:2]
    if result == 'A' or result == 'B':
        return result
    return data[-7].split(' ')[-4][1:2]

def run_matches(teamA, teamB, score):
    curTA = teamA
    curTB = teamB

    map_score = {}
    for map in maps:
        
        print('Running map {}'.format(map))
        processes.add(subprocess.Popen('gradlew run -Pmaps={map} -PteamA={a} -PteamB={b} -Pdebug=false -Penableprofiler=false -PoutputVerbose=false -PshowIndicators=false -Dbc.server.websocket=false'.format(map = map, a = teamA, b = teamB), stdout=subprocess.PIPE, text=True, shell=True))
        print(len(processes))
        while(len(processes) >= max_subprocesses):
            time.sleep(1)
            to_remove = []
            for p in processes:
                if p.poll() is not None:
                    out, err = p.communicate()
                    #print(out)
                    #print(err)
                    winner = get_winner(out.split('\n'))
                    if winner == 'A':
                        score[0] += 1
                        map_score[map] = teamA
                    elif winner == 'B':
                        score[1] += 1
                        map_score[map] = teamB
                    else:
                        print('I don\'t know who won this, take a look: {}'.format(out))
                    current_score = score
                    current_map_score = map_score
                    to_remove.append(p)
            for p in to_remove:
                processes.remove(p)
        
    while(len(processes) > 0):
        time.sleep(1)
        for p in processes:
            if p.poll() is not None:
                out = p.output.read().splitlines()
                winner = get_winner(out)
                if winner == 'A':
                    score[0] += 1
                    map_score[map] = teamA
                elif winner == 'B':
                    score[1] += 1
                    map_score[map] = teamB
                else:
                    print('I don\'t know who won this, take a look: {}'.format(out))
                processes.remove(p)
                current_score = score
                current_map_score = map_score


    return score, map_score

def sigint_handler(sig, frame):
    print('Sigint received, exiting...')
    for p in processes:
        p.kill()
    print('Current score: {} {} {}'.format(current_score, curTA, curTB)) # borked, does not actually update
    print(current_map_score)
    exit()

def main():
    os.chdir('..')
    os.system('setx JAVA_HOME "C:\Program Files\Java\jdk1.8.0_351"')
    os.system('gradlew update')
    os.system('gradlew build')

    signal.signal(signal.SIGINT, sigint_handler)

    current = time.time()
    score1 = run_matches(teamA, teamB, [0, 0])
    print('Match 1 score ({} vs {}): {}'.format(teamA, teamB, score1[0]))
    print('Match 1 maps: {}'.format(score1[1]))

    current_score = []
    current_map_score = {}

    score2 = run_matches(teamB, teamA, [0, 0])
    print('Match 2 score ({} vs {}): {}'.format(teamB, teamA, score2[0]))
    print('Match 2 maps: {}'.format(score2[1]))
    final_score = [score1[0][0] + score2[0][1], score1[0][1] + score2[0][0]]
    print('Final score ({} vs {}): {}'.format(teamA, teamB, final_score))

    for key in score1[1].keys():
        if score1[1][key] == score2[1][key]:
            print('{} won both sides on {}'.format(score1[1][key], key))

    print('Total runtime was {} seconds'.format(time.time() - current))

if __name__ == '__main__':
    main()